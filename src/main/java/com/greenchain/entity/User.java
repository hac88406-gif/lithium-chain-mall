package com.greenchain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String phone;

    private String email;

    private String avatar;

    private Integer status;

    /**
     * 角色：前台客户固定为 USER，数据库 user 表无此列，
     * 标记为非持久化字段，避免 MyBatis-Plus 自动 SELECT role 列报 Unknown column。
     */
    @TableField(exist = false)
    private String role;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}