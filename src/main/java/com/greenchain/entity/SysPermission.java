package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 系统权限实体
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /**
     * 权限ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 父权限ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 权限名称
     */
    @TableField("name")
    private String name;

    /**
     * 权限标识
     */
    @TableField("permission_key")
    private String permissionKey;

    /**
     * 类型 1菜单 2按钮
     */
    @TableField("type")
    private Integer type;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;
}