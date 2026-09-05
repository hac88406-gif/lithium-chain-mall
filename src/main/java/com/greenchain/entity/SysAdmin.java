package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统管理员实体
 */
@Data
@TableName("sys_admin")
public class SysAdmin {

    /**
     * 管理员ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

    /**
     * 密码（加密）
     */
    @TableField("password")
    private String password;

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 状态 0禁用 1启用
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}