package com.greenchain.interceptor;

import com.greenchain.annotation.RequiresPermission;
import com.greenchain.common.BusinessException;
import com.greenchain.common.PermissionContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限拦截器
 * 用于拦截请求并校验权限
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查是否为方法处理器
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查方法上是否有权限注解
        RequiresPermission annotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (annotation == null) {
            return true;
        }

        // 获取需要的权限
        String requiredPermission = annotation.value();

        // 检查权限
        if (!PermissionContext.hasPermission(requiredPermission)) {
            throw new BusinessException(403, "没有访问权限");
        }

        return true;
    }
}