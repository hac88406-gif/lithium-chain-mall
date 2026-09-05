package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.util.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 幂等令牌发放控制器
 * <p>
 * 鉴权：归属 /api/client/** 拦截范围，未加入拦截器排除路径，
 * 必须携带有效 JWT token（ClientAuthInterceptor），userId 从 JWT 解析获取。
 * <p>
 * 令牌机制：Redis key = idempotent:token:{uuid}，value = userId（用于切面归属校验），
 * TTL 5 分钟；业务接口请求头携带 Idempotent-Token: {uuid}，
 * 由 IdempotentAspect 校验并原子消费。
 */
@RestController
@RequestMapping("/api/client/idempotent")
public class IdempotentController {

    /** 幂等令牌 Redis key 前缀（与 IdempotentAspect 保持一致） */
    private static final String TOKEN_KEY_PREFIX = "idempotent:token:";

    /** 令牌有效期（分钟） */
    private static final long TOKEN_TTL_MINUTES = 5L;

    @Autowired
    private CacheUtil cacheUtil;

    /**
     * 获取幂等令牌
     * 前端在提交受 @IdempotentToken 保护的接口（如创建订单）前调用，
     * 拿到令牌后放入请求头 Idempotent-Token。
     *
     * @param userId 当前登录用户ID（JWT 解析，同时作为令牌归属标识写入 Redis）
     * @return Result<String> 幂等令牌（uuid）
     */
    @GetMapping("/token")
    public Result<String> getToken(@RequestAttribute("userId") Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        // value 存 userId，切面消费时校验令牌归属，防止跨用户使用令牌
        cacheUtil.set(TOKEN_KEY_PREFIX + token, userId, TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return Result.success("获取成功", token);
    }
}
