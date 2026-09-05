package com.greenchain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.greenchain.entity.SysAdmin;

import java.util.Set;

/**
 * 管理员服务接口（仅保留登录鉴权所需方法）
 */
public interface SysAdminService extends IService<SysAdmin> {

    /**
     * 根据用户名查找管理员（登录用）
     */
    SysAdmin findByUsername(String username);

    /**
     * 获取管理员权限集合（登录 + 鉴权拦截器用）
     */
    Set<String> getAdminPermissions(Long adminId);
}
