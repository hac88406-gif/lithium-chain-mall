package com.greenchain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenchain.config.DifyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify AI 智能客服服务
 * <p>
 * 对接本地 Dify 部署（localhost:8088），Dify 内部配置双轨模型：
 * <ol>
 *   <li>#1 deepseek-chat（云端主力，质量好速度快）</li>
 *   <li>#2 qwen2.5:3b（Ollama 本地，零成本可离线）</li>
 * </ol>
 * 后端只调 Dify 一个接口，模型切换逻辑完全封装在 Dify 内部。
 * <p>
 * Dify Agent 类型应用只支持 streaming（SSE 流）模式，不支持 blocking。
 * 所以实现为原生 HttpURLConnection 逐行读 SSE 流，拼接 answer 得到完整回复。
 */
@Slf4j
@Service
public class DifyService {

    @Autowired private DifyConfig difyConfig;
    @Autowired private ObjectMapper objectMapper;

    /**
     * 智能客服对话入口
     *
     * @param userId  用户唯一标识（会话 ID）
     * @param message 用户输入消息
     * @return Dify AI 完整回复文本（异常时抛给上层 catch）
     */
    public String chat(String userId, String message) throws Exception {
        if (userId == null || userId.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            return "";
        }

        String url = difyConfig.getBaseUrl() + "/chat-messages";
        String apiKey = difyConfig.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("[DifyService] API Key 未配置");
            throw new RuntimeException("Dify API Key 未配置");
        }

        // 构造请求体
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", new HashMap<>());
        body.put("query", message);
        body.put("response_mode", "streaming");
        body.put("user", userId);
        String jsonBody = objectMapper.writeValueAsString(body);

        log.debug("[DifyService] 调用 Dify，url={}, user={}, body={}", url, userId, jsonBody);

        // 用原生 HttpURLConnection（RestTemplate 处理 SSE 流不方便）
        URL connUrl = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) connUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(difyConfig.getTimeoutSeconds() * 1000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        // 写请求体
        try (var os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.warn("[DifyService] HTTP {}，错误：{}", code, err);
            throw new RuntimeException("Dify HTTP " + code + ": " + err);
        }

        // 读 SSE 流，拼接 answer
        StringBuilder answer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // SSE 格式：每行 "data: {JSON}" 或空行或 "data: [DONE]"
                if (!line.startsWith("data:")) continue;
                String json = line.substring(5).trim();
                if ("[DONE]".equals(json)) continue;

                JsonNode node = objectMapper.readTree(json);
                String event = node.path("event").asText("");
                if ("agent_message".equals(event)) {
                    String chunk = node.path("answer").asText("");
                    if (!chunk.isEmpty()) {
                        answer.append(chunk);
                    }
                }
            }
        }

        String result = answer.toString().trim();
        log.debug("[DifyService] Dify 回复成功，长度={} 字符", result.length());
        return result;
    }
}
