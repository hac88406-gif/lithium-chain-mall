package com.greenchain.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解（Redis 令牌机制，防止重复提交）
 * <p>
 * 使用方式：
 * 1. 前端先调用 GET /api/client/idempotent/token 获取幂等令牌（Redis 存 5 分钟）；
 * 2. 提交业务请求时在请求头携带 Idempotent-Token: {token}；
 * 3. 切面（IdempotentAspect）校验并原子消费令牌：
 *    - 令牌存在 → 删除令牌（DEL 原子操作，并发下仅一个请求成功），放行执行业务；
 *    - 令牌不存在 / 已被消费 → 直接返回 400"请勿重复提交请求"，业务不执行。
 * <p>
 * 令牌生命周期由切面管理：业务执行成功后令牌销毁；
 * 业务执行失败（异常或业务失败码）时令牌自动回补，允许用户修正后重试。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IdempotentToken {
}
