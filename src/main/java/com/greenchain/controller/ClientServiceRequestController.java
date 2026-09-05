package com.greenchain.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greenchain.common.Result;
import com.greenchain.entity.CustomerServiceRequest;
import com.greenchain.entity.ServiceRequestMessage;
import com.greenchain.entity.ServiceRequestReview;
import com.greenchain.mapper.CustomerServiceRequestMapper;
import com.greenchain.mapper.ServiceRequestMessageMapper;
import com.greenchain.mapper.ServiceRequestReviewMapper;
import com.greenchain.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 客户端提交人工客服/留言请求 + AI转人工双向聊天。
 * 允许未登录提交，但登录用户带 token 时会自动解析 userId 用于会话关联。
 */
@RestController
@RequestMapping("/api/client/service-request")
@RequiredArgsConstructor
public class ClientServiceRequestController {

    private final CustomerServiceRequestMapper requestMapper;
    private final ServiceRequestMessageMapper messageMapper;
    private final ServiceRequestReviewMapper reviewMapper;
    private final JwtUtil jwtUtil;

    /**
     * 尝试从请求头解析 JWT 拿到 userId；没 token / token 无效返回 null（不拦截，允许未登录）。
     * 替代 ClientAuthInterceptor（service-request 已被排除拦截，需要手动解析）。
     */
    private Long resolveUserId(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                return Long.parseLong(jwtUtil.getUserIdFromToken(token));
            }
        } catch (Exception ignored) { /* token 无效或缺失 → 按未登录处理 */ }
        return null;
    }

    /** 提交工单（含幂等 + AI转人工） */
    @PostMapping("/submit")
    public Result<CustomerServiceRequest> submit(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {

        Long userId = resolveUserId(request);

        // ===== 幂等第一层：chat_session_id 命中（同浏览器 localStorage 持久化） =====
        String sessionId = (String) body.get("chatSessionId");
        if (sessionId != null && !sessionId.isBlank()) {
            CustomerServiceRequest existing = requestMapper.selectOne(
                    new LambdaQueryWrapper<CustomerServiceRequest>()
                            .eq(CustomerServiceRequest::getChatSessionId, sessionId)
                            .in(CustomerServiceRequest::getStatus, 0, 1) // 0=待处理 1=处理中：已解决/已关闭一律不复用
                            .last("LIMIT 1"));
            if (existing != null) {
                return Result.success(existing);
            }
        }

        // ===== 幂等第二层：userId 兜底（换浏览器/清 localStorage 仍可复用未结束工单） =====
        if (userId != null) {
            CustomerServiceRequest existing = requestMapper.selectOne(
                    new LambdaQueryWrapper<CustomerServiceRequest>()
                            .eq(CustomerServiceRequest::getUserId, userId)
                            .in(CustomerServiceRequest::getStatus, 0, 1) // 仅待处理/处理中复用
                            .orderByDesc(CustomerServiceRequest::getCreateTime)
                            .last("LIMIT 1"));
            if (existing != null) {
                return Result.success(existing);
            }
        }

        CustomerServiceRequest r = new CustomerServiceRequest();
        r.setUserId(userId);
        r.setName((String) body.get("name"));
        r.setPhone((String) body.get("phone"));
        r.setEmail((String) body.get("email"));
        r.setSource(toString(body.get("source"), "contact"));
        r.setQuestion((String) body.get("question"));
        r.setContent((String) body.get("content"));
        r.setChatSessionId(sessionId);
        r.setChatHistory((String) body.get("chatHistory"));
        r.setStatus(0);
        r.setAdminUnreadCount(1); // 首条即未读
        r.setCreateTime(LocalDateTime.now());
        r.setUpdateTime(LocalDateTime.now());
        requestMapper.insert(r);

        // 首条系统提示消息（客户请求转人工）
        if ("ai_transfer".equals(r.getSource())) {
            ServiceRequestMessage sys = new ServiceRequestMessage();
            sys.setRequestId(r.getId());
            sys.setSenderType("system");
            sys.setContent("【客户从AI转人工】请客服查看上下文后回复");
            sys.setIsRead(0);
            messageMapper.insert(sys);
        }
        return Result.success(r);
    }

    /** 客户发送一条消息 */
    @PostMapping("/{id}/reply")
    public Result<ServiceRequestMessage> customerReply(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> body) {
        CustomerServiceRequest req = requestMapper.selectById(id);
        if (req == null) return Result.error(404, "工单不存在");
        // 状态 2=超时关闭 3=归档完成：均禁止继续发消息
        if (req.getStatus() != null && (req.getStatus() == 2 || req.getStatus() == 3)) {
            return Result.error(400, "会话已关闭，请重新发起咨询");
        }
        ServiceRequestMessage msg = new ServiceRequestMessage();
        msg.setRequestId(id);
        msg.setSenderType("customer");
        msg.setContent((String) body.get("content"));
        msg.setIsRead(0);
        messageMapper.insert(msg);
        // 未读计数 +1 + 自动置为对话活跃中
        requestMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CustomerServiceRequest>()
                .eq(CustomerServiceRequest::getId, id)
                .setSql("admin_unread_count = COALESCE(admin_unread_count, 0) + 1")
                .set(CustomerServiceRequest::getStatus, 1)
                .set(CustomerServiceRequest::getUpdateTime, LocalDateTime.now()));
        return Result.success(msg);
    }

    /** 客户拉取消息（增量 lastMsgId） */
    @GetMapping("/{id}/messages")
    public Result<List<ServiceRequestMessage>> pullMessages(
            @PathVariable Long id,
            @RequestParam(required = false) Long lastMsgId) {
        LambdaQueryWrapper<ServiceRequestMessage> w = new LambdaQueryWrapper<>();
        w.eq(ServiceRequestMessage::getRequestId, id);
        if (lastMsgId != null) {
            w.gt(ServiceRequestMessage::getId, lastMsgId);
        }
        w.orderByAsc(ServiceRequestMessage::getId);
        return Result.success(messageMapper.selectList(w));
    }

    /** 客户端：获取工单详情（恢复会话时需要 status 等） */
    @GetMapping("/{id}")
    public Result<CustomerServiceRequest> detail(@PathVariable Long id) {
        return Result.success(requestMapper.selectById(id));
    }

    /** 客户标记已读（把管理员发来的消息置为已读） */
    @PutMapping("/{id}/read")
    public Result<Void> markClientRead(@PathVariable Long id) {
        messageMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ServiceRequestMessage>()
                .eq(ServiceRequestMessage::getRequestId, id)
                .eq(ServiceRequestMessage::getSenderType, "admin")
                .eq(ServiceRequestMessage::getIsRead, 0)
                .set(ServiceRequestMessage::getIsRead, 1));
        return Result.success();
    }

    @GetMapping("/mine")
    public Result<List<CustomerServiceRequest>> mine(HttpServletRequest request) {
        Long userId = resolveUserId(request);
        if (userId == null) return Result.success(List.of());
        return Result.success(requestMapper.selectList(
                new LambdaQueryWrapper<CustomerServiceRequest>()
                        .eq(CustomerServiceRequest::getUserId, userId)
                        .orderByDesc(CustomerServiceRequest::getCreateTime)));
    }

    /** 闲置超时自动关闭（前端 10 秒无消息触发，将工单置为 2=超时关闭 等待客户评价） */
    @PutMapping("/{id}/timeout-close")
    public Result<Void> timeoutClose(@PathVariable Long id) {
        CustomerServiceRequest req = requestMapper.selectById(id);
        if (req == null) return Result.error(404, "工单不存在");
        // 已超时 / 已归档：不重复操作
        if (req.getStatus() != null && (req.getStatus() == 2 || req.getStatus() == 3)) {
            return Result.success();
        }
        requestMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CustomerServiceRequest>()
                .eq(CustomerServiceRequest::getId, id)
                .in(CustomerServiceRequest::getStatus, 0, 1)
                .set(CustomerServiceRequest::getStatus, 2)
                .set(CustomerServiceRequest::getUpdateTime, LocalDateTime.now()));
        return Result.success();
    }

    /** 客户提交评价 → 工单自动进入 3=归档完成（一个工单只允许一条评价） */
    @PostMapping("/{id}/review")
    public Result<ServiceRequestReview> submitReview(@PathVariable Long id,
                                                     @RequestBody Map<String, Object> body) {
        CustomerServiceRequest req = requestMapper.selectById(id);
        if (req == null) return Result.error(404, "工单不存在");
        // 只有超时关闭（2）或已经归档（3）的工单可以提交评价
        if (req.getStatus() == null || (req.getStatus() != 2 && req.getStatus() != 3)) {
            return Result.error(400, "会话未结束，暂不能评价");
        }
        // 已评价过直接返回（幂等）
        ServiceRequestReview existing = reviewMapper.selectOne(
                new LambdaQueryWrapper<ServiceRequestReview>()
                        .eq(ServiceRequestReview::getRequestId, id));
        if (existing != null) {
            return Result.success(existing);
        }
        ServiceRequestReview review = new ServiceRequestReview();
        review.setRequestId(id);
        int rating = ((Number) body.getOrDefault("rating", 5)).intValue();
        review.setRating(Math.max(1, Math.min(5, rating))); // 钳制 1-5
        review.setContent(toString(body.get("content"), ""));
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);

        // 工单归档（3=已完成）
        requestMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<CustomerServiceRequest>()
                .eq(CustomerServiceRequest::getId, id)
                .set(CustomerServiceRequest::getStatus, 3)
                .set(CustomerServiceRequest::getUpdateTime, LocalDateTime.now()));
        return Result.success(review);
    }

    /** 查询某个工单的评价（客户恢复会话时 / 管理端详情展示时使用） */
    @GetMapping("/{id}/review")
    public Result<ServiceRequestReview> getReview(@PathVariable Long id) {
        ServiceRequestReview r = reviewMapper.selectOne(
                new LambdaQueryWrapper<ServiceRequestReview>()
                        .eq(ServiceRequestReview::getRequestId, id));
        return Result.success(r);
    }

    private String toString(Object v, String def) {
        return v == null ? def : v.toString();
    }
}
