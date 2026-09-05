package com.greenchain.dto.response;

import lombok.Data;

import java.util.Set;

/**
 * 管理员登录响应VO
 * 追加当前账号权限集合，不破坏原有返回结构
 */
@Data
public class AdminLoginVO {

    /**
     * 管理员ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * JWT token
     */
    private String token;

    /**
     * 权限集合（新增字段）
     */
    private Set<String> permissions;
}