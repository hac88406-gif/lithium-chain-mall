package com.greenchain.controller;

import com.greenchain.common.BusinessException;
import com.greenchain.common.PermissionContext;
import com.greenchain.common.Result;
import com.greenchain.dto.request.LoginRequest;
import com.greenchain.dto.response.AdminLoginVO;
import com.greenchain.entity.SysAdmin;
import com.greenchain.entity.SysRole;
import com.greenchain.mapper.SysRoleMapper;
import com.greenchain.service.SysAdminService;
import com.greenchain.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 管理员认证控制器
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    @Autowired
    private SysAdminService adminService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 查找管理员
        SysAdmin admin = adminService.findByUsername(username);
        if (admin == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 检查状态
        if (admin.getStatus() != 1) {
            throw new BusinessException(401, "账号已被禁用");
        }

        // 密码验证（B1 修复：恢复 BCrypt 校验，之前被注释导致知道用户名即可登录）
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 获取权限集合
        Set<String> permissions = adminService.getAdminPermissions(admin.getId());

        // 获取角色名称
        String roleName = null;
        if (admin.getRoleId() != null) {
            SysRole role = roleMapper.selectById(admin.getRoleId());
            if (role != null) {
                roleName = role.getRoleName();
            }
        }

        // 构建响应
        AdminLoginVO vo = new AdminLoginVO();
        vo.setId(admin.getId());
        vo.setUsername(admin.getUsername());
        vo.setRoleId(admin.getRoleId());
        vo.setRoleName(roleName);
        vo.setToken(generateToken(admin));
        vo.setPermissions(permissions);

        return Result.success("登录成功", vo);
    }

    /**
     * JWT 工具：签发管理员 token（B2 修复：明文 token 可伪造，改为签名 JWT）
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 生成管理员登录 token（JWT）
     * claims 含 adminId、username、role="ADMIN"，有效期 24 小时；
     * 由 AdminAuthInterceptor 校验签名并解析出管理员身份。
     */
    private String generateToken(SysAdmin admin) {
        return jwtUtil.generateAdminToken(admin.getId(), admin.getUsername());
    }

    /**
     * 获取当前登录管理员信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("id", PermissionContext.getAdminId());
        info.put("username", PermissionContext.getUsername());
        info.put("permissions", PermissionContext.getPermissions());
        info.put("isSuperAdmin", PermissionContext.isSuperAdmin());
        return Result.success(info);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // 在实际应用中，这里应该清除服务器端的token缓存
        return Result.success("退出成功");
    }
}