package com.moxi.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Python Agent 服务 HTTP 客户端。
 *
 * <p>封装对独立部署的 moxi-agent（默认端口 8001）的调用，支持：</p>
 * <ul>
 *   <li>{@link #generateSync(AgentRequest)}：同步生成，返回完整结果；</li>
 *   <li>{@link #generateStream(AgentRequest, BiConsumer)}：SSE 流式生成；</li>
 *   <li>{@link #chatStream(String, String, String, BiConsumer)}：SSE 流式对话；</li>
 *   <li>{@link #isAvailable()}：健康检查。</li>
 * </ul>
 *
 * <p>注意：Java camelCase 字段通过 {@link AgentRequest}/{@link AgentResponse}
 * 上的 {@code @JsonNaming} 自动与 Python snake_case 字段互转。</p>
 */
@Slf4j
@Component
public class AgentClient {

    @Value("${agent.python-service-url:http://localhost:8001}")
    private String agentServiceUrl;

    @Value("${agent.timeout-ms:120000}")
    private int timeoutMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AgentClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步调用 Agent 生成内容。
     *
     * @param request 生成请求
     * @return 生成结果
     */
    public AgentResponse generateSync(AgentRequest request) {
        String url = agentServiceUrl + "/api/agent/generate/sync";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AgentRequest> entity = new HttpEntity<>(request, headers);
        log.debug("Agent generateSync request, url={}, taskType={}", url, request.getTaskType());
        return restTemplate.postForObject(url, entity, AgentResponse.class);
    }

    /**
     * 流式调用 Agent 生成内容（SSE）。
     *
     * @param request      生成请求
     * @param eventHandler 事件回调 (eventType, jsonData) -> void
     */
    public void generateStream(AgentRequest request, BiConsumer<String, String> eventHandler) {
        String url = agentServiceUrl + "/api/agent/generate";
        String body = writeJson(request);
        consumeSse(url, body, eventHandler);
    }

    /**
     * 流式对话（SSE）。
     *
     * @param userId       用户 ID
     * @param sessionId    会话 ID
     * @param message      用户消息
     * @param eventHandler 事件回调 (eventType, jsonData) -> void
     */
    public void chatStream(String userId, String sessionId, String message,
                           BiConsumer<String, String> eventHandler) {
        String url = agentServiceUrl + "/api/agent/chat";
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("user_id", userId);
        payload.put("session_id", sessionId);
        payload.put("message", message);
        String body = writeJson(payload);
        consumeSse(url, body, eventHandler);
    }

    /**
     * 健康检查：探测 Python Agent 服务是否可用。
     *
     * @return 可用返回 true，否则 false
     */
    @SuppressWarnings("rawtypes")
    public boolean isAvailable() {
        try {
            restTemplate.getForObject(agentServiceUrl + "/health", Map.class);
            return true;
        } catch (Exception e) {
            log.warn("Agent service unavailable: {}", e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 内部工具方法
    // ------------------------------------------------------------------

    /**
     * 以 SSE 方式消费指定 URL：POST 请求体，逐行解析事件流。
     */
    private void consumeSse(String url, String requestBody, BiConsumer<String, String> eventHandler) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "text/event-stream");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            if (stream == null) {
                log.error("Agent SSE empty response, url={}, code={}", url, code);
                eventHandler.accept("error", "{\"code\":\"AGENT_ERROR\",\"message\":\"空响应\"}");
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                if (code < 200 || code >= 300) {
                    String errBody = readAll(reader);
                    log.error("Agent SSE http error, url={}, code={}, body={}", url, code, errBody);
                    eventHandler.accept("error",
                            "{\"code\":\"AGENT_HTTP_" + code + "\",\"message\":" + quote(errBody) + "}");
                    return;
                }
                AgentStreamHandler.handle(reader, eventHandler);
            }
        } catch (Exception e) {
            log.error("Agent SSE consume failed, url={}", url, e);
            eventHandler.accept("error",
                    "{\"code\":\"AGENT_ERROR\",\"message\":" + quote(e.getMessage()) + "}");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 Agent 请求体失败", e);
        }
    }

    private String readAll(BufferedReader reader) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    /**
     * 将任意字符串安全地包装成 JSON 字符串字面量。
     */
    private String quote(String raw) {
        if (raw == null) {
            return "\"\"";
        }
        try {
            return objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            return "\"" + raw.replace("\"", "'") + "\"";
        }
    }
}
