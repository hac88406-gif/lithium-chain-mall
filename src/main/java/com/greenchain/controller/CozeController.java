package com.greenchain.controller;

import com.greenchain.common.Result;
import com.greenchain.service.DifyService;
import com.greenchain.service.FaqService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Dify 绿链锂电智能客服接口（公开接口，未登录也能发起对话）
 * <p>
 * 架构：请求 → Dify（deepseek 云端主力 + Ollama 本地 3B 双轨）→ FAQ 关键词库兜底
 * 路径保持 /api/client/coze/** 不变（WebMvcConfig 白名单 + 前端不用改）。
 * <p>
 * 鉴权说明：/api/client/coze/** 已加入 ClientAuthInterceptor 白名单，
 * 未登录用户可访问（前端悬浮聊天窗未登录也能用）。
 * 已登录用户前端会携带 userId，后端优先使用真实 userId 传给 Dify；
 * 未登录用户使用前端生成的 sessionId 作为 Dify 的 user。
 * <p>
 * 返回扩展：除了 AI 回复文本 answer，还会返回 needManual 布尔字段，
 * 当 AI 回复命中转人工关键词时置为 true，前端据此弹出人工售后工单入口。
 */
@Slf4j
@Api(tags = "AI智能客服 - Dify智能体小绿（云端deepseek+本地Ollama双轨）")
@RestController
@RequestMapping("/api/client/coze")
public class CozeController {

    /** Dify 流式 AI 服务（localhost:8088）：先走云端 deepseek，失败回退 Ollama 本地 */
    @Autowired
    private DifyService difyService;

    /** 本地 FAQ 关键词库：Dify 全挂或返回空时的最终兜底，保证任何环境都有回复 */
    @Autowired
    private FaqService faqService;

    /**
     * 智能客服对话（公开接口，无需登录；Dify Agent 流式 SSE 接口）
     * <p>
     * 请求体 JSON：
     * <pre>
     * {
     *   "sessionId": "uuid-xxx",        // 会话唯一标识，前端生成 UUID 存 localStorage（必填）
     *   "userQuestion": "我要退款",       // 用户输入问题文本（必填）
     *   "userId": "1"                   // 已登录用户真实数据库 ID（可选，未登录时不传）
     * }
     * </pre>
     * 返回 Result 结构：
     * <pre>
     * {
     *   "code": 200,
     *   "data": {
     *     "answer": "绿链锂电AI回复文本...",
     *     "needManual": false
     *   },
     *   "message": "success"
     * }
     * </pre>
     *
     * @param params 请求参数：sessionId + userQuestion + 可选 userId
     * @return 客服回复（Dify 大模型 → 本地 FAQ 兜底；永不为 null；扩展 needManual 标记）
     */
    @ApiOperation(value = "AI智能客服对话", notes = "调用 Dify Agent 获取 SSE 流式回复（Dify 内部双轨：云端 deepseek + Ollama 本地 qwen2.5:3b）。Dify 失败或返回空时自动降级 FAQ 关键词库。返回 answer 为 AI 回复文本，needManual=true 表示回复命中转人工关键词。已登录用户可带 userId 让 AI 知道身份，未登录也能聊（用 sessionId 兜底）。")
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(
            @ApiParam(value = "请求体，含 sessionId/userQuestion/可选 userId", required = true)
            @RequestBody Map<String, String> params) {

        // 1. 参数提取与校验
        String sessionId = params.get("sessionId");
        String userQuestion = params.get("userQuestion");
        // 可选 userId（前端登录后从 localStorage.userInfo.id 传过来；未登录时为 null）
        String userId = params.get("userId");

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return Result.<Map<String, Object>>error(400, "sessionId 不能为空");
        }
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return Result.<Map<String, Object>>error(400, "userQuestion 不能为空");
        }

        // 2. 决定传给 Dify 的 user：有真实 userId 用 userId，否则用 sessionId
        //    这样登录场景 AI 知道是谁，未登录场景也能正常对话
        String difyUser = (userId != null && !userId.trim().isEmpty()) ? userId.trim() : sessionId;
        log.debug("[DifyChat] sessionId={}, userId={}, difyUser={}, question={}",
                sessionId, userId, difyUser, userQuestion);

        // 3. 调用 DifyService（走 SSE 流拼接）；失败或返回空 → 降级 FAQ
        String answer;
        try {
            answer = difyService.chat(difyUser, userQuestion);
            if (answer == null || answer.trim().isEmpty()) {
                log.warn("[DifyChat] Dify 返回空，降级 FAQ");
                answer = faqService.chat(userQuestion);
            }
        } catch (Exception e) {
            log.warn("[DifyChat] Dify 调用失败，降级 FAQ：{}", e.getMessage());
            answer = faqService.chat(userQuestion);
        }

        // 4. 关键词检测：AI 回复命中转人工关键词 → needManual=true
        boolean needManual = detectManualTransfer(answer);
        if (needManual) {
            log.debug("[DifyChat] AI 回复命中转人工关键词，needManual=true");
        }

        // 5. 组装扩展返回结构：answer + needManual
        Map<String, Object> data = new HashMap<>(2);
        data.put("answer", answer);
        data.put("needManual", needManual);

        return Result.success(data);
    }

    /**
     * 检测 AI 回复是否命中转人工关键词
     * <p>
     * 匹配规则：回复文本（转小写后）包含任一"引导用户转人工"的关键词即判定为命中。
     * 注意：必须排除 AI 正常政策解答里天然包含的"客服电话/客服热线"等词，
     * 否则用户问退款问题 AI 正常回答后反而会被误判为"需要转人工"。
     *
     * @param answer AI 回复文本
     * @return 是否命中转人工关键词
     */
    private boolean detectManualTransfer(String answer) {
        if (answer == null || answer.isEmpty()) return false;
        String lower = answer.toLowerCase();
        // 只匹配 AI 主动引导转人工的话术（"请/建议/联系/转接 + 人工"），
        // 避开政策解答里天然带的"客服电话/客服热线/联系我们"
        return lower.contains("转人工")
                || lower.contains("联系人工客服")
                || lower.contains("请联系人工")
                || lower.contains("转接人工")
                || lower.contains("联系专业客服")
                || lower.contains("建议联系人工")
                || lower.contains("人工客服为您")
                || lower.contains("工单转人工")
                || lower.contains("转客服")
                || lower.contains("需要人工处理");
    }
}
