package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.entity.BusinessCooperation;
import com.greenchain.mapper.BusinessCooperationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 客户端提交商务合作 / 媒体采访申请。
 * 允许未登录提交（userId 为空即可），但登录后会自动关联 userId。
 */
@RestController
@RequestMapping("/api/client/cooperation")
@RequiredArgsConstructor
public class ClientCooperationController {

    private final BusinessCooperationMapper coopMapper;

    /**
     * 提交商务合作申请（客户端 Cooperation.vue submitForm 调用）
     */
    @PostMapping("/submit")
    public Result<BusinessCooperation> submit(
            @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody Map<String, Object> body) {

        BusinessCooperation c = new BusinessCooperation();
        c.setUserId(userId);
        c.setType(toString(body.get("type"), "cooperation"));
        c.setCompanyName((String) body.get("companyName"));
        c.setCompanyAddress((String) body.get("companyAddress"));
        c.setContactPerson((String) body.get("contactPerson"));
        c.setPosition((String) body.get("position"));
        c.setPhone((String) body.get("phone"));
        c.setEmail((String) body.get("email"));
        c.setRequirementType((String) body.get("requirementType"));
        c.setBudget((String) body.get("budget"));
        c.setIntention((String) body.get("intention"));
        c.setDeliveryCycle((String) body.get("deliveryCycle"));
        c.setMediaTitle((String) body.get("mediaTitle"));
        c.setMediaFormat((String) body.get("mediaFormat"));
        c.setMediaDate((String) body.get("mediaDate"));
        c.setRemark((String) body.get("remark"));
        c.setStatus(0);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        coopMapper.insert(c);
        return Result.success(c);
    }

    /**
     * 我的商务合作申请（已登录用户查看自己的历史提交）
     */
    @GetMapping("/mine")
    public Result<List<BusinessCooperation>> mine(
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Result.success(List.of());
        }
        return Result.success(
                coopMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BusinessCooperation>()
                                .eq(BusinessCooperation::getUserId, userId)
                                .orderByDesc(BusinessCooperation::getCreateTime)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private String toString(Object v, String def) {
        if (v == null) return def;
        if (v instanceof List) {
            return ((List<Object>) v).stream().map(Object::toString).collect(java.util.stream.Collectors.joining(","));
        }
        return v.toString();
    }
}
