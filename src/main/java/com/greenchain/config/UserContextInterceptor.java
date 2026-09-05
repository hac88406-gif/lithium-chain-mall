package com.greenchain.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 简单用户上下文拦截器
 * 从请求头X-User-Id中获取用户ID，放入request属性
 * 课程作业简化版：不使用Spring Security，通过请求头传递用户身份
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);
                request.setAttribute("userId", userId);
            } catch (NumberFormatException e) {
                // 忽略格式错误
            }
        } else {
            // 默认给一个测试用户ID
            request.setAttribute("userId", 2L);
        }
        return true;
    }
}