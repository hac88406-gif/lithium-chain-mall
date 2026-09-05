package com.greenchain.interceptor;

import com.greenchain.common.BusinessException;
import com.greenchain.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 下单接口 IP 限流拦截器
 * <p>
 * 防脚本刷单：同一 IP 地址 1 分钟内最多允许 5 次下单请求。
 * 底层用 Redis INCR 计数器（固定窗口算法）实现，原子操作、性能 O(1)。
 * <p>
 * 设计要点：
 * ① 只拦截下单接口（由 WebMvcConfig 精确指定路径），不影响其他 API
 * ② IP 取不到时降级放行（内网/本地测试场景不报 429）
 * ③ 限流原因写 WARN 日志，方便定位恶意 IP
 */
@Slf4j
@Component
public class OrderRateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long WINDOW_SECONDS = 60;
    private static final String KEY_PREFIX = "rate_limit:order:ip:";

    @Autowired
    private CacheUtil cacheUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = extractClientIp(request);
        if (ip == null || ip.isEmpty()) {
            // IP 取不到（极端情况），降级放行，不要误伤
            return true;
        }

        String key = KEY_PREFIX + ip;
        boolean allowed = cacheUtil.tryAcquire(key, MAX_REQUESTS_PER_MINUTE, WINDOW_SECONDS);

        if (!allowed) {
            log.warn("下单限流拦截：IP={} 超过 1 分钟 {} 次阈值, URI={}",
                    ip, MAX_REQUESTS_PER_MINUTE, request.getRequestURI());
            throw new BusinessException(429, "操作太频繁，请稍后再试");
        }
        return true;
    }

    /**
     * 从请求中提取客户端真实 IP
     * <p>
     * 支持 Nginx 反向代理场景（X-Forwarded-For / X-Real-IP），
     * 多层代理时取第一个可信 IP。没有代理时 fallback 到 request.getRemoteAddr()。
     */
    private String extractClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For 可能是 "client, proxy1, proxy2"，取第一个
                int commaIdx = value.indexOf(',');
                return commaIdx > 0 ? value.substring(0, commaIdx).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
