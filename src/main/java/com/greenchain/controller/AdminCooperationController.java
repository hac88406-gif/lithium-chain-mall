package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.Result;
import com.greenchain.entity.BusinessCooperation;
import com.greenchain.mapper.BusinessCooperationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理端：商务合作/媒体采访申请管理
 */
@RestController
@RequestMapping("/api/admin/cooperation")
@RequiredArgsConstructor
public class AdminCooperationController {

    private final BusinessCooperationMapper mapper;

    @GetMapping("/page")
    public Result<Page<BusinessCooperation>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BusinessCooperation> w = new LambdaQueryWrapper<>();
        if (status != null) w.eq(BusinessCooperation::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(BusinessCooperation::getCompanyName, keyword)
                    .or().like(BusinessCooperation::getContactPerson, keyword)
                    .or().like(BusinessCooperation::getPhone, keyword));
        }
        w.orderByDesc(BusinessCooperation::getCreateTime);
        return Result.success(mapper.selectPage(new Page<>(page, size), w));
    }

    @GetMapping("/{id}")
    public Result<BusinessCooperation> detail(@PathVariable Long id) {
        return Result.success(mapper.selectById(id));
    }

    /**
     * 管理员回复 / 更新状态（跟进、关闭）
     */
    @PutMapping("/{id}/reply")
    public Result<BusinessCooperation> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BusinessCooperation c = mapper.selectById(id);
        if (c == null) return Result.error(404, "申请不存在");
        if (body.containsKey("status")) {
            c.setStatus(((Number) body.get("status")).intValue());
        }
        if (body.containsKey("reply")) {
            c.setReply((String) body.get("reply"));
            c.setReplyTime(LocalDateTime.now());
        }
        c.setReplyBy(toString(body.get("replyBy"), "admin"));
        c.setUpdateTime(LocalDateTime.now());
        mapper.updateById(c);
        return Result.success(c);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }

    private String toString(Object v, String def) {
        return v == null ? def : v.toString();
    }
}
