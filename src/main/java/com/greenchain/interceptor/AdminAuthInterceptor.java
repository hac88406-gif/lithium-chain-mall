package com.greenchain.interceptor;

import com.greenchain.common.PermissionContext;
import com.greenchain.entity.SysAdmin;
import com.greenchain.mapper.SysAdminMapper;
import com.greenchain.service.SysAdminService;
import com.greenchain.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * 管理后台鉴权拦截器（B1 修复）
 * <p>
 * 拦截 /api/admin/** 下除登录接口外的所有请求：
 * 1. 从 Authorization 头解析 Bearer JWT（AdminAuthController 登录时签发）；
 * 2. 校验签名/有效期/role=ADMIN，失败统一返回 body code=401（HTTP 保持 200，与 ClientAuthInterceptor 风格一致）；
 * 3. 反查 sys_admin 确认账号存在且未禁用（status=1）；
 * 4. 加载权限集合写入 PermissionContext，供 PermissionInterceptor 的 @RequiresPermission 校验；
 * 5. afterCompletion 阶段清理 PermissionContext，修复 ThreadLocal 在线程池复用下的泄漏。
 * <p>
 * 注册顺序（WebMvcConfig）：AdminAuthInterceptor（鉴权）→ PermissionInterceptor（权限）。
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    /** JWT 工具：校验并解析管理员 token */
    private final JwtUtil jwtUtil;

    /** 管理员 Mapper：反查账号存在性与启用状态 */
    private final SysAdminMapper sysAdminMapper;

    /** 管理员 Service：加载权限集合 */
    private final SysAdminService sysAdminService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");

        // ---- 1. token 缺失或格式错误 → 401 ----
        if (token == null || !token.startsWith("Bearer ") || token.substring(7).trim().isEmpty()) {
            return unauthorized(response, "请先登录");
        }
        token = token.substring(7).trim();

        // ---- 2. JWT 校验（签名/过期/role=ADMIN），失败 → 401 ----
        if (!jwtUtil.validateAdminToken(token)) {
            return unauthorized(response, "请先登录");
        }

        // ---- 3. 解析 adminId 并反查账号（存在 + 未禁用） ----
        Long adminId;
        try {
            adminId = jwtUtil.getAdminIdFromToken(token);
        } catch (Exception e) {
            return unauthorized(response, "请先登录");
        }

        SysAdmin admin = sysAdminMapper.selectById(adminId);
        if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
            return unauthorized(response, "账号已被禁用");
        }

        // ---- 4. 加载权限集合写入上下文，并暴露 adminId 给后续 Controller ----
        Set<String> permissions = sysAdminService.getAdminPermissions(adminId);
        PermissionContext.setAdmin(adminId, admin.getUsername(), permissions);
        request.setAttribute("adminId", adminId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 无论业务成功与否都清理 ThreadLocal，防止线程池复用串号/内存泄漏
        PermissionContext.clear();
    }

    /**
     * 统一写 401 JSON 响应（HTTP 状态保持 200，业务 code=401，与客户端拦截器风格一致）
     *
     * @return 恒为 false，终止后续处理
     */
    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
        return false;
    }
}
