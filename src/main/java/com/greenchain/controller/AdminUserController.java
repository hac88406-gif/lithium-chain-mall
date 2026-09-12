package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.annotation.RequiresPermission;
import com.greenchain.common.Result;
import com.greenchain.dto.response.PageResult;
import com.greenchain.entity.User;
import com.greenchain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理后台 - 买家用户管理
 * 路径：/api/admin/user，权限标识与 schema.sql sys_permission.id=15-17 对齐：
 * sys:user:list（查看列表）、sys:user:edit（编辑/禁用）、重置密码复用 sys:user:edit。
 * <p>
 * 说明：分页返回前统一将 password 置空，避免密码哈希通过接口泄露给前端。
 */
@RestController
@RequestMapping("/api/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 分页查询买家用户（GET /api/admin/user/page?current=1&size=10&keyword=&status=）
     * keyword 模糊匹配用户名/昵称/手机号；status 可选 1=正常 0=禁用。
     */
    @GetMapping("/page")
    @RequiresPermission("sys:user:list")
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);

        IPage<User> pageResult = userMapper.selectPage(new Page<>(page, size), wrapper);
        // 脱敏：不向前端返回密码哈希
        pageResult.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(PageResult.of(pageResult));
    }

    /**
     * 更新买家用户（PUT /api/admin/user/{id}）
     * 支持：status（1=正常 0=禁用）、nickname（昵称）。禁用后用户无法再登录（见 ClientAuthController.login 状态校验）。
     */
    @PutMapping("/{id}")
    @RequiresPermission("sys:user:edit")
    public Result<User> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if (body.containsKey("status")) {
            Object statusObj = body.get("status");
            if (statusObj == null) {
                return Result.error(400, "状态值不能为空");
            }
            int status = ((Number) statusObj).intValue();
            if (status != 0 && status != 1) {
                return Result.error(400, "状态值不合法，仅支持 0=禁用 / 1=正常");
            }
            user.setStatus(status);
        }
        if (body.containsKey("nickname")) {
            Object nickname = body.get("nickname");
            if (nickname != null && !nickname.toString().isBlank()) {
                user.setNickname(nickname.toString());
            }
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        user.setPassword(null);
        return Result.success("更新成功", user);
    }

    /**
     * 重置买家用户密码（POST /api/admin/user/{id}/reset-password）
     * body: { "password": "新密码" }，长度 6~20，后端 BCrypt 加密存储。
     */
    @PostMapping("/{id}/reset-password")
    @RequiresPermission("sys:user:edit")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        Object pwdObj = body.get("password");
        String newPwd = pwdObj == null ? null : pwdObj.toString();
        if (newPwd == null || newPwd.isBlank() || newPwd.length() < 6 || newPwd.length() > 20) {
            return Result.error(400, "密码长度需为 6~20 位");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success("密码重置成功");
    }
}
