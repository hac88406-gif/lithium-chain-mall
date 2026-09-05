package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.greenchain.common.PermissionContext;
import com.greenchain.entity.SysAdmin;
import com.greenchain.entity.SysPermission;
import com.greenchain.mapper.SysAdminMapper;
import com.greenchain.mapper.SysPermissionMapper;
import com.greenchain.service.SysAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理员服务实现类（仅保留登录鉴权所需方法）
 */
@Service
public class SysAdminServiceImpl extends ServiceImpl<SysAdminMapper, SysAdmin> implements SysAdminService {

    @Autowired
    private SysAdminMapper adminMapper;

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Override
    public SysAdmin findByUsername(String username) {
        LambdaQueryWrapper<SysAdmin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAdmin::getUsername, username);
        return adminMapper.selectOne(wrapper);
    }

    @Override
    public Set<String> getAdminPermissions(Long adminId) {
        SysAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            return Collections.emptySet();
        }

        // 超级管理员 admin：直接返回全权限通配符 "*"，PermissionContext.hasPermission 会通配放行
        // 不查 sys_permission 表（系统后台已下线，表可能不存在）
        if (PermissionContext.SUPER_ADMIN_USERNAME.equals(admin.getUsername())) {
            return Set.of("*");
        }

        // 普通管理员（如 operator）：查权限表；表不存在则返回空集合并由 PermissionInterceptor 拦截
        if (admin.getRoleId() == null) {
            return Collections.emptySet();
        }
        try {
            List<SysPermission> permissions = permissionMapper.selectPermissionsByRoleId(admin.getRoleId());
            return permissions.stream()
                .map(SysPermission::getPermissionKey)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            // 权限表不存在时兜底返回空集，避免 Spring 启动失败
            return Collections.emptySet();
        }
    }
}
