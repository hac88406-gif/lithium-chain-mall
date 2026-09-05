package com.greenchain.aspect;

import com.greenchain.annotation.IdempotentToken;
import com.greenchain.util.CacheUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * IdempotentAspect 单元测试
 * <p>
 * 重点测试 AOP 环绕通知的完整链路：
 *   ① 请求头缺失 → 拦截
 *   ② 令牌不存在/已消费 → 拦截
 *   ③ 令牌归属校验失败 → 拦截
 *   ④ 正常消费令牌 → 业务执行 → 成功后令牌销毁
 *   ⑤ 业务返回失败 → 令牌回补允许重试
 *   ⑥ 业务抛异常 → 令牌回补
 * <p>
 * 每个方法内部直接创建 mock 对象（@Nested 子类不继承父类 @BeforeEach）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotentAspect - 幂等令牌切面测试")
class IdempotentAspectTest {

    @Mock
    private CacheUtil cacheUtil;

    @InjectMocks
    private IdempotentAspect idempotentAspect;

    // ==================== 拦截场景 ====================

    @Nested
    @DisplayName("拦截场景 - 请求不合法时应该被拦截")
    class InterceptScenarios {

        @Test
        @DisplayName("请求头缺失 Idempotent-Token → 返回 400 拦截")
        void should_intercept_when_header_missing() throws Throwable {
            // 每个方法独立创建 mock（@Nested 子类不继承父 @BeforeEach）
            HttpServletRequest request = mock(HttpServletRequest.class);
            IdempotentToken annotation = mock(IdempotentToken.class);
            org.aspectj.lang.ProceedingJoinPoint pjp = mock(org.aspectj.lang.ProceedingJoinPoint.class);
            ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
            when(attrs.getRequest()).thenReturn(request);
            RequestContextHolder.setRequestAttributes(attrs);

            // given：请求头为空
            when(request.getHeader("Idempotent-Token")).thenReturn(null);
            when(request.getRequestURI()).thenReturn("/api/client/order/create");

            // when
            Object result = idempotentAspect.around(pjp, annotation);

            // then：业务没执行，返回 400
            verify(pjp, never()).proceed();
            assertTrue(result instanceof Map);
            assertEquals(400, ((Map<?, ?>) result).get("code"));
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("令牌不存在于 Redis（已被消费或过期）→ 返回 400 拦截")
        void should_intercept_when_token_not_in_redis() throws Throwable {
            HttpServletRequest request = mock(HttpServletRequest.class);
            IdempotentToken annotation = mock(IdempotentToken.class);
            org.aspectj.lang.ProceedingJoinPoint pjp = mock(org.aspectj.lang.ProceedingJoinPoint.class);
            ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
            when(attrs.getRequest()).thenReturn(request);
            RequestContextHolder.setRequestAttributes(attrs);

            // given：Redis 查不到令牌
            when(request.getHeader("Idempotent-Token")).thenReturn("token-abc-123");
            when(request.getRequestURI()).thenReturn("/api/client/order/create");
            when(cacheUtil.get("idempotent:token:token-abc-123")).thenReturn(null);

            Object result = idempotentAspect.around(pjp, annotation);

            verify(pjp, never()).proceed();
            assertTrue(result instanceof Map);
            assertEquals(400, ((Map<?, ?>) result).get("code"));
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("令牌归属校验失败（别人的令牌被复用）→ 返回 400 拦截")
        void should_intercept_when_token_owner_mismatch() throws Throwable {
            HttpServletRequest request = mock(HttpServletRequest.class);
            IdempotentToken annotation = mock(IdempotentToken.class);
            org.aspectj.lang.ProceedingJoinPoint pjp = mock(org.aspectj.lang.ProceedingJoinPoint.class);
            ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
            when(attrs.getRequest()).thenReturn(request);
            RequestContextHolder.setRequestAttributes(attrs);

            // given：令牌属于 userId=999，但当前请求的 userId 是 200
            when(request.getHeader("Idempotent-Token")).thenReturn("token-abc-123");
            when(request.getAttribute("userId")).thenReturn(200L);
            when(cacheUtil.get("idempotent:token:token-abc-123")).thenReturn(999L);

            Object result = idempotentAspect.around(pjp, annotation);

            verify(pjp, never()).proceed();
            assertTrue(result instanceof Map);
            assertEquals(400, ((Map<?, ?>) result).get("code"));
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("令牌已被并发消费（deleteIfPresent 返回 false）→ 返回 400 拦截")
        void should_intercept_when_token_already_consumed() throws Throwable {
            HttpServletRequest request = mock(HttpServletRequest.class);
            IdempotentToken annotation = mock(IdempotentToken.class);
            org.aspectj.lang.ProceedingJoinPoint pjp = mock(org.aspectj.lang.ProceedingJoinPoint.class);
            ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
            when(attrs.getRequest()).thenReturn(request);
            RequestContextHolder.setRequestAttributes(attrs);

            // given：Redis 查得到令牌，但 DEL 返回 false → 被并发请求消费了
            when(request.getHeader("Idempotent-Token")).thenReturn("token-abc-123");
            when(cacheUtil.get("idempotent:token:token-abc-123")).thenReturn(200L);
            when(cacheUtil.deleteIfPresent("idempotent:token:token-abc-123")).thenReturn(false);

            Object result = idempotentAspect.around(pjp, annotation);

            verify(pjp, never()).proceed();
            assertTrue(result instanceof Map);
            assertEquals(400, ((Map<?, ?>) result).get("code"));
            RequestContextHolder.resetRequestAttributes();
        }
    }

    // ==================== 放行业务 ====================

    @Nested
    @DisplayName("放行业务 - 请求合法时应该通过")
    class PassScenarios {

        private HttpServletRequest request;
        private IdempotentToken annotation;
        private org.aspectj.lang.ProceedingJoinPoint pjp;

        /** 准备一个合法请求上下文：令牌存在 + 归属正确 + 未被消费 */
        private void setupValidRequestContext() {
            request = mock(HttpServletRequest.class);
            annotation = mock(IdempotentToken.class);
            pjp = mock(org.aspectj.lang.ProceedingJoinPoint.class);
            ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
            when(attrs.getRequest()).thenReturn(request);
            RequestContextHolder.setRequestAttributes(attrs);

            when(request.getHeader("Idempotent-Token")).thenReturn("token-valid-001");
            when(request.getAttribute("userId")).thenReturn(200L);
            when(cacheUtil.get("idempotent:token:token-valid-001")).thenReturn(200L);
            when(cacheUtil.deleteIfPresent("idempotent:token:token-valid-001")).thenReturn(true);
        }

        @Test
        @DisplayName("正常消费令牌 → 业务执行成功，令牌已销毁")
        void should_pass_and_destroy_token_on_success() throws Throwable {
            setupValidRequestContext();

            Map<String, Object> successResult = new HashMap<>();
            successResult.put("code", 200);
            successResult.put("message", "下单成功");
            when(pjp.proceed()).thenReturn(successResult);

            Object result = idempotentAspect.around(pjp, annotation);

            assertEquals(successResult, result);
            verify(pjp, times(1)).proceed();
            verify(cacheUtil, times(1)).deleteIfPresent("idempotent:token:token-valid-001");
            // 业务成功 → 令牌已消费，不回补
            verify(cacheUtil, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("业务返回 code!=200 → 令牌回补，允许用户修正后重试")
        void should_refund_token_when_business_returns_error() throws Throwable {
            setupValidRequestContext();

            Map<String, Object> failResult = new HashMap<>();
            failResult.put("code", 400);
            failResult.put("message", "库存不足");
            when(pjp.proceed()).thenReturn(failResult);

            Object result = idempotentAspect.around(pjp, annotation);

            assertEquals(failResult, result);
            verify(pjp, times(1)).proceed();
            // 业务失败 → 令牌回补
            verify(cacheUtil, times(1)).set(
                    eq("idempotent:token:token-valid-001"),
                    eq(200L),
                    eq(5L),
                    eq(TimeUnit.MINUTES));
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("业务抛异常 → 令牌回补，异常继续向上抛")
        void should_refund_token_and_rethrow_on_exception() {
            setupValidRequestContext();
            try {
                when(pjp.proceed()).thenThrow(new RuntimeException("DB timeout"));
            } catch (Throwable ignored) {
                // thenThrow 的参数是 checked Throwable，ProceedingJoinPoint.proceed() 声明的就是 throws Throwable
            }

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                idempotentAspect.around(pjp, annotation);
            });

            assertTrue(thrown.getMessage().contains("DB timeout"));
            // 令牌被回补
            verify(cacheUtil, times(1)).set(
                    eq("idempotent:token:token-valid-001"),
                    eq(200L),
                    eq(5L),
                    eq(TimeUnit.MINUTES));
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
