package com.greenchain.service;

import com.greenchain.dto.request.LoginRequest;
import com.greenchain.dto.response.UserLoginVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录（简化版：直接校验用户名密码）
     */
    UserLoginVO login(LoginRequest request);
}