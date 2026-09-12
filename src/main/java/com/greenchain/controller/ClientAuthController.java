package com.greenchain.controller;

import com.greenchain.entity.User;
import com.greenchain.mapper.UserMapper;
import com.greenchain.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/client/auth")
public class ClientAuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        String username = request.get("username");
        String password = request.get("password");
        String phone = request.get("phone");
        String email = request.get("email");

        if (userMapper.findByUsername(username) != null) {
            result.put("code", 400);
            result.put("message", "用户名已存在");
            return result;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        result.put("code", 200);
        result.put("message", "注册成功");
        result.put("data", user);
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        String username = request.get("username");
        String password = request.get("password");

        User user = userMapper.findByUsername(username);

        if (user == null) {
            result.put("code", 400);
            result.put("message", "用户名或密码错误");
            return result;
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.put("code", 400);
            result.put("message", "用户名或密码错误");
            return result;
        }

        // 禁用校验：管理后台将用户 status 置 0 后，禁止该账号继续登录
        if (user.getStatus() != null && user.getStatus() != 1) {
            result.put("code", 400);
            result.put("message", "账号已被禁用，请联系客服");
            return result;
        }

        String token = jwtUtil.generateToken(user.getId().toString(), "USER");

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        result.put("code", 200);
        result.put("message", "登录成功");
        result.put("data", data);
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退出成功");
        return result;
    }
}