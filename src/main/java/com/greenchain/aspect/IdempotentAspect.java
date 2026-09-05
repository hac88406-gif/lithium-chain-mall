package com.greenchain.aspect;

import com.greenchain.annotation.IdempotentToken;
import com.greenchain.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 幂等令牌切面（AOP 环绕通知）
 * <p>
 * 拦截所有标注 @IdempotentToken 的接口，基于 Redis 令牌实现防重复提交：
 * ① 校验请求头 Idempotent-Token，缺失直接拦截；
 * ② 校验令牌归属（令牌 value 存储发放时的 userId，防止使用他人令牌）；
 * ③ 原子消费令牌（Redis DEL，并发下仅一个请求删除成功）后放行业务；
 * ④ 业务执行成功 → 令牌已销毁，重复请求被拦截；
 *    业务执行失败（异常或业务失败码）→ 令牌自动回补（重新写入 5 分钟 TTL），允许用户重试；
 * ⑤ 不侵入业务代码：被拦截方法的业务逻辑完全不动。
 */
@Slf4j
@Aspect
@Component
public class IdempotentAspect {

    /** 幂等令牌 Redis key 前缀 */
    private static final String TOKEN_KEY_PREFIX = "idempotent:token:";

    /** 令牌有效期（分钟） */
    private static final long TOKEN_TTL_MINUTES = 5L;

    /** 请求头名称 */
    private static final String TOKEN_HEADER = "Idempotent-Token";

    @Autowired
    private CacheUtil cacheUtil;

    @Around("@annotation(idempotentToken)")
    public Object around(ProceedingJoinPoint pjp, IdempotentToken idempotentToken) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // ① 请求头缺失 → 视为未按规范调用，直接拦截
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.trim().isEmpty()) {
            log.warn("幂等校验拦截：请求 {} 未携带 Idempotent-Token", request.getRequestURI());
            return failResult();
        }

        String key = TOKEN_KEY_PREFIX + token.trim();

        // ② 校验令牌归属：value 为发放令牌时的 userId，防止使用他人令牌
        Object owner = cacheUtil.get(key);
        if (owner == null) {
            // 令牌不存在 / 已过期 / 已被消费 → 重复提交
            log.warn("幂等校验拦截：令牌无效或已被消费，uri={}", request.getRequestURI());
            return failResult();
        }
        Object userId = request.getAttribute("userId");
        if (userId != null && !String.valueOf(owner).equals(String.valueOf(userId))) {
            log.warn("幂等校验拦截：令牌归属校验失败，tokenOwner={}，currentUser={}", owner, userId);
            return failResult();
        }

        // ③ 原子消费令牌：Redis DEL 是原子操作，并发重复请求中只有一个能删除成功
        boolean consumed = cacheUtil.deleteIfPresent(key);
        if (!consumed) {
            // 并发场景：另一请求已消费该令牌
            log.warn("幂等校验拦截：令牌已被并发请求消费，uri={}", request.getRequestURI());
            return failResult();
        }

        // ④ 放行业务执行（不修改任何业务逻辑）
        try {
            Object result = pjp.proceed();

            // 业务返回统一结构的失败码（如库存不足 400）→ 令牌回补，允许修正后重试
            if (result instanceof Map) {
                Object code = ((Map<?, ?>) result).get("code");
                if (code instanceof Number && ((Number) code).intValue() != 200) {
                    cacheUtil.set(key, owner, TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
                    log.info("业务失败，幂等令牌已回补：{}", key);
                }
            }
            return result;
        } catch (Throwable e) {
            // 业务异常 → 令牌回补，允许用户重试
            cacheUtil.set(key, owner, TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("业务异常，幂等令牌已回补：{}", key);
            throw e;
        }
    }

    /**
     * 拦截响应：与 Controller 现有 Map 风格返回结构保持一致（code/message）
     */
    private Map<String, Object> failResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("message", "请勿重复提交请求");
        return result;
    }
}
