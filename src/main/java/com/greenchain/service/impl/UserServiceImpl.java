package com.greenchain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greenchain.common.BusinessException;
import com.greenchain.dto.request.LoginRequest;
import com.greenchain.dto.response.UserLoginVO;
import com.greenchain.entity.User;
import com.greenchain.mapper.UserMapper;
import com.greenchain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserLoginVO login(LoginRequest request) {
        // 参数校验
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        // 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 校验密码（课程作业简化版：直接明文比对）
        if (!user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }

        // 转换为登录响应VO
        UserLoginVO vo = new UserLoginVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        // 前台客户角色固定 USER（user 表无 role 列，实体该字段为非持久化）
        vo.setRole("USER");
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        // 简化版token：用userId生成一个简单token
        vo.setToken("token_" + user.getId() + "_" + System.currentTimeMillis());

        log.info("用户登录成功：{}", user.getUsername());
        return vo;
    }
}