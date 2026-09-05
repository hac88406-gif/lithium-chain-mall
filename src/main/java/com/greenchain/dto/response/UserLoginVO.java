package com.greenchain.dto.response;

import lombok.Data;

/**
 * 用户登录响应VO
 */
@Data
public class UserLoginVO {

    private Long id;

    private String username;

    private String role;

    private String email;

    private String phone;

    /** 简化版token：直接返回用户信息字符串 */
    private String token;
}