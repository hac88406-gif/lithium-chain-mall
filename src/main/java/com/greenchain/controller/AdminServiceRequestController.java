package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greenchain.common.Result;
import com.greenchain.entity.CustomerServiceRequest;
import com.greenchain.entity.ServiceRequestMessage;
import com.greenchain.entity.ServiceRequestReview;
import com.greenchain.entity.User;
import com.greenchain.mapper.CustomerServiceRequestMapper;
import com.greenchain.mapper.ServiceRequestMessageMapper;
import com.greenchain.mapper.ServiceRequestReviewMapper;
import com.greenchain.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理端：人工客服/留言请求管理 + 双向聊天消息
 */
@RestController
@RequestMapping("/api/admin/service-request")
@RequiredArgsConstructor
public class AdminServiceRequestController {

    private final CustomerServiceRequestMapper requestMapper;
    private final ServiceRequestMessageMapper messageMapper;
    private final ServiceRequestReviewMapper reviewMapper;
    private final UserMapper userMapper;

    @GetMapping("/page")
    public Result<Page<CustomerServiceRequest>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) List<Integer> statusList,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer adminUnreadOnly) {
        LambdaQueryWrapper<CustomerServiceRequest> w = new LambdaQueryWrapper<>();
        if (status != null) {
            w.eq(CustomerServiceRequest::getStatus, status);
        } else if (statusList != null && !statusList.isEmpty()) {
            w.in(CustomerServiceRequest::getStatus, statusList);
        }
        if (adminUnreadOnly != null && adminUnreadOnly == 1) {
            w.gt(CustomerServiceRequest::getAdminUnreadCount, 0);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(CustomerServiceRequest::getName, keyword)
                    .or().like(CustomerServiceRequest::getPhone, keyword)
                    .or().like(CustomerServiceRequest::getContent, keyword));
        }
        w.orderByDesc(CustomerServiceRequest::getCreateTime);
        Page<CustomerServiceRequest> p = requestMapper.selectPage(new Page<>(page, size), w);
        for (CustomerServiceRequest r : p.getRecords()) fillUserInfo(r);
        return Result.success(p);
    }

    @GetMapping("/{id}")
    public Result<CustomerServiceRequest> detail(@PathVariable Long id) {
        CustomerServiceRequest r = requestMapper.selectById(id);
        if (r != null) fillUserInfo(r);
        return Result.success(r);
    }

    /** 从 user 表按 userId 回填客户姓名/电话/邮箱（如果原工单没填） */
    private void fillUserInfo(CustomerServiceRequest r) {
        if (r == null || r.getUserId() == null) return;
        boolean needFill = r.getName() == null || r.getName().isBlank()
                || r.getPhone() == null || r.getPhone().isBlank()
                || r.getEmail() == null || r.getEmail().isBlank();
        if (!needFill) return;
        User u = userMapper.selectById(r.getUserId());
        if (u == null) return;
        if (r.getName() == null || r.getName().isBlank()) {
            r.setName(u.getNickname() != null && !u.getNickname().isBlank() ? u.getNickname() : u.getUsername());
        }
        if (r.getPhone() == null || r.getPhone().isBlank()) {
            r.setPhone(u.getPhone());
        }
        if (r.getEmail() == null || r.getEmail().isBlank()) {
            r.setEmail(u.getEmail());
        }
    }

    /** 管理端查看完整消息列表 */
    @GetMapping("/{id}/messages")
    public Result<List<ServiceRequestMessage>> messages(@PathVariable Long id,
                                                        @RequestParam(required = false) Long lastMsgId) {
        LambdaQueryWrapper<ServiceRequestMessage> w = new LambdaQueryWrapper<>();
        w.eq(ServiceRequestMessage::getRequestId, id);
        if (lastMsgId != null) {
            w.gt(ServiceRequestMessage::getId, lastMsgId);
        }
        w.orderByAsc(ServiceRequestMessage::getId);
        return Result.success(messageMapper.selectList(w));
    }

    /** 管理端回复（发一条 admin 消息）
     * 规则：管理员无权手动修改工单状态；只有工单处于 0=待应答 或 1=活跃中 才能发消息。
     *      若工单已是 0=待应答，回复后自动升级为 1=活跃中（系统自动行为）。
     *      2=超时关闭 / 3=归档完成 均禁止发送。
     */
    @PostMapping("/{id}/reply")
    public Result<ServiceRequestMessage> adminReply(@PathVariable Long id,
                                                    @RequestBody Map<String, Object> body) {
        CustomerServiceRequest req = requestMapper.selectById(id);
        if (req == null) return Result.error(404, "工单不存在");
        int s = req.getStatus() == null ? 0 : req.getStatus();
        if (s == 2 || s == 3) {
            return Result.error(400, "会话已关闭，无法继续回复");
        }
        ServiceRequestMessage msg = new ServiceRequestMessage();
        msg.setRequestId(id);
        msg.setSenderType("admin");
        msg.setContent((String) body.get("content"));
        msg.setIsRead(0);
        messageMapper.insert(msg);

        // 同步更新工单（不再接受前端 status 字段：管理员无权手动完结工单）
        CustomerServiceRequest update = new CustomerServiceRequest();
        update.setId(id);
        update.setReply((String) body.get("content"));
        update.setReplyBy(toString(body.get("replyBy"), "admin"));
        update.setReplyTime(LocalDateTime.now());
        if (s == 0) update.setStatus(1); // 自动升级为活跃中
        update.setUpdateTime(LocalDateTime.now());
        requestMapper.updateById(update);
        return Result.success(msg);
    }

    /** 管理端查询某个工单的客户评价 */
    @GetMapping("/{id}/review")
    public Result<ServiceRequestReview> getReview(@PathVariable Long id) {
        ServiceRequestReview r = reviewMapper.selectOne(
                new LambdaQueryWrapper<ServiceRequestReview>()
                        .eq(ServiceRequestReview::getRequestId, id));
        return Result.success(r);
    }

    /** 管理端标记已读（清 admin_unread_count + 把 customer 的未读消息置 1） */
    @PutMapping("/{id}/read")
    public Result<Void> markAdminRead(@PathVariable Long id) {
        requestMapper.update(null, new LambdaUpdateWrapper<CustomerServiceRequest>()
                .eq(CustomerServiceRequest::getId, id)
                .set(CustomerServiceRequest::getAdminUnreadCount, 0));
        messageMapper.update(null, new LambdaUpdateWrapper<ServiceRequestMessage>()
                .eq(ServiceRequestMessage::getRequestId, id)
                .eq(ServiceRequestMessage::getSenderType, "customer")
                .eq(ServiceRequestMessage::getIsRead, 0)
                .set(ServiceRequestMessage::getIsRead, 1));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 级联删除消息
        messageMapper.delete(new LambdaQueryWrapper<ServiceRequestMessage>()
                .eq(ServiceRequestMessage::getRequestId, id));
        requestMapper.deleteById(id);
        return Result.success();
    }

    private String toString(Object v, String def) {
        return v == null ? def : v.toString();
    }
}
