package com.greenchain.common;

import java.util.Set;

/**
 * 权限上下文工具类
 * 用于存储当前登录管理员的信息和权限集合
 */
public class PermissionContext {

    /**
     * 超级管理员用户名（豁免所有权限校验）
     */
    public static final String SUPER_ADMIN_USERNAME = "admin";

    /**
     * ThreadLocal存储当前管理员ID
     */
    private static final ThreadLocal<Long> ADMIN_ID = new ThreadLocal<>();

    /**
     * ThreadLocal存储当前管理员用户名
     */
    private static final ThreadLocal<String> ADMIN_USERNAME = new ThreadLocal<>();

    /**
     * ThreadLocal存储当前管理员权限集合
     */
    private static final ThreadLocal<Set<String>> PERMISSIONS = new ThreadLocal<>();

    /**
     * 设置当前管理员信息
     */
    public static void setAdmin(Long adminId, String username, Set<String> permissions) {
        ADMIN_ID.set(adminId);
        ADMIN_USERNAME.set(username);
        PERMISSIONS.set(permissions);
    }

    /**
     * 获取当前管理员ID
     */
    public static Long getAdminId() {
        return ADMIN_ID.get();
    }

    /**
     * 获取当前管理员用户名
     */
    public static String getUsername() {
        return ADMIN_USERNAME.get();
    }

    /**
     * 获取当前管理员权限集合
     */
    public static Set<String> getPermissions() {
        return PERMISSIONS.get();
    }

    /**
     * 判断是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        String username = ADMIN_USERNAME.get();
        return SUPER_ADMIN_USERNAME.equals(username);
    }

    /**
     * 检查是否拥有指定权限
     */
    public static boolean hasPermission(String permissionKey) {
        // 超级管理员拥有所有权限
        if (isSuperAdmin()) {
            return true;
        }

        Set<String> permissions = PERMISSIONS.get();
        if (permissions == null) {
            return false;
        }
        // 通配符 "*" 表示全权限（admin 账号兜底）
        if (permissions.contains("*")) {
            return true;
        }
        return permissions.contains(permissionKey);
    }

    /**
     * 清除当前上下文
     */
    public static void clear() {
        ADMIN_ID.remove();
        ADMIN_USERNAME.remove();
        PERMISSIONS.remove();
    }
}