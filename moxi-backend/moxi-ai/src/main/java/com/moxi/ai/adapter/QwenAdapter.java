package com.moxi.ai.adapter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.moxi.ai.dto.AIRequest;
import com.moxi.ai.dto.AIResponse;
import com.moxi.common.exception.BusinessException;
import com.moxi.common.response.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * 通义千问（DashScope）适配器实现。
 *
 * <p>真实模式下通过 {@link RestTemplate} 调用阿里云 DashScope 文本生成接口；
 * 开发阶段默认走 Mock 模式，按 genType 返回结构化的模拟内容，
 * 以便在不依赖外部 API 的情况下打通全链路。</p>
 *
 * <p>真实调用受 Resilience4j 熔断器 {@code qwen} 保护，
 * 熔断或调用异常时触发 fallback，抛出 {@link ErrorCode#AI_SERVICE_UNAVAILABLE}。</p>
 */
@Slf4j
@Component
public class QwenAdapter implements AIProviderAdapter {

    private static final String PROVIDER_NAME = "qwen";
    private static final String PROVIDER_MOCK = "mock";
    private static final String GENERATION_PATH = "/chat/completions";

    private static final String FIELD_TOTAL_TOKENS = "total_tokens";
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_CHOICES = "choices";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_OUTPUT = "output";
    private static final String FIELD_USAGE = "usage";

    @Value("${ai.qwen.api-key:}")
    private String apiKey;

    @Value("${ai.qwen.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Value("${ai.qwen.model:qwen-turbo}")
    private String model;

    @Value("${ai.qwen.timeout-ms:30000}")
    private int timeoutMs;

    @Value("${ai.qwen.enabled:false}")
    private boolean enabled;

    @Value("${ai.mock.enabled:true}")
    private boolean mockEnabled;

    private final RestTemplate restTemplate;

    public QwenAdapter() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 字段注入完成后，按配置项设置 RestTemplate 超时。
     */
    @PostConstruct
    void initTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate.setRequestFactory(factory);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    @CircuitBreaker(name = PROVIDER_NAME, fallbackMethod = "generateFallback")
    public AIResponse generate(AIRequest request) {
        if (shouldUseMock()) {
            return generateMockResponse(request);
        }
        return callQwenApi(request);
    }

    @Override
    @CircuitBreaker(name = PROVIDER_NAME, fallbackMethod = "generateStreamFallback")
    public AIResponse generateStream(AIRequest request, Consumer<String> chunkConsumer) {
        if (shouldUseMock()) {
            AIResponse resp = generateMockResponse(request);
            chunkConsumer.accept(resp.getContent());
            return resp;
        }
        return callQwenApiStream(request, chunkConsumer);
    }

    /**
     * 流式调用的兜底方法。
     */
    private AIResponse generateStreamFallback(AIRequest request, Consumer<String> chunkConsumer, Throwable t) {
        log.error("Qwen stream fallback triggered, genType={}, reason={}", request.getGenType(), t.getMessage());
        throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    /**
     * 熔断/调用失败时的兜底方法，直接抛出业务异常，
     * 由上层统一处理。
     */
    private AIResponse generateFallback(AIRequest request, Throwable t) {
        log.error("Qwen generate fallback triggered, genType={}, reason={}", request.getGenType(), t.getMessage());
        throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Override
    public boolean isAvailable() {
        return enabled || mockEnabled;
    }

    @Override
    public float getLoad() {
        if (shouldUseMock()) {
            return 0.0f;
        }
        return ThreadLocalRandom.current().nextFloat();
    }

    // ------------------------------------------------------------------
    // 真实调用
    // ------------------------------------------------------------------

    /**
     * 调用 OpenAI 兼容的 chat completions 接口。
     *
     * <p>请求体遵循 OpenAI 格式：
     * <pre>
     * {
     *   "model": "LongCat-2.0",
     *   "messages": [{"role":"user","content":"..."}],
     *   "temperature": 0.7,
     *   "max_tokens": 2048
     * }
     * </pre>
     * 响应解析取 {@code choices[0].message.content}，
     * token 数取自 {@code usage.total_tokens}。</p>
     */
    private AIResponse callQwenApi(AIRequest request) {
        long start = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", request.getPrompt());
        messages.add(userMessage);

        Map<String, Object> body = new HashMap<>();
        body.put("model", StrUtil.isNotBlank(request.getModel()) ? request.getModel() : model);
        body.put("messages", messages);
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2048);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = StrUtil.removeSuffix(baseUrl, "/") + GENERATION_PATH;

        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (RestClientException e) {
            log.error("Call AI API failed, url={}, msg={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        long duration = System.currentTimeMillis() - start;

        String responseBody = response.getBody();
        if (StrUtil.isBlank(responseBody)) {
            log.warn("AI API returns empty body, url={}", url);
            return AIResponse.builder()
                    .content(StrUtil.EMPTY)
                    .tokensUsed(0)
                    .provider(PROVIDER_NAME)
                    .durationMs(duration)
                    .build();
        }

        JSONObject json = JSONUtil.parseObj(responseBody);
        String content = extractContent(json);
        int tokensUsed = extractTokens(json);

        log.info("AI generate done, genType={}, tokens={}, durationMs={}",
                request.getGenType(), tokensUsed, duration);

        return AIResponse.builder()
                .content(content)
                .tokensUsed(tokensUsed)
                .provider(PROVIDER_NAME)
                .durationMs(duration)
                .build();
    }

    /**
     * 流式调用 OpenAI 兼容接口（stream=true）。
     *
     * <p>使用 HttpURLConnection 读取 SSE 响应，逐 chunk 提取
     * {@code choices[0].delta.content} 并回调 chunkConsumer。
     * LongCat-2.0 等推理模型的 {@code reasoning_content} 不转发。</p>
     */
    private AIResponse callQwenApiStream(AIRequest request, Consumer<String> chunkConsumer) {
        long start = System.currentTimeMillis();

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", request.getPrompt());
        messages.add(userMessage);

        Map<String, Object> body = new HashMap<>();
        body.put("model", StrUtil.isNotBlank(request.getModel()) ? request.getModel() : model);
        body.put("messages", messages);
        body.put("temperature", request.getTemperature() != null ? request.getTemperature() : 0.7);
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 2048);
        body.put("stream", true);

        String urlStr = StrUtil.removeSuffix(baseUrl, "/") + GENERATION_PATH;
        StringBuilder contentBuilder = new StringBuilder();
        int tokensUsed = 0;

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Accept", "text/event-stream");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(JSONUtil.toJsonStr(body).getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                log.error("Stream API returned non-200: code={}, url={}", code, urlStr);
                throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JSONObject json = JSONUtil.parseObj(data);
                        JSONArray choices = json.getJSONArray(FIELD_CHOICES);
                        if (choices != null && !choices.isEmpty()) {
                            JSONObject firstChoice = choices.getJSONObject(0);
                            if (firstChoice != null) {
                                JSONObject delta = firstChoice.getJSONObject("delta");
                                if (delta != null) {
                                    String content = delta.getStr(FIELD_CONTENT);
                                    if (StrUtil.isNotBlank(content)) {
                                        contentBuilder.append(content);
                                        chunkConsumer.accept(content);
                                    }
                                }
                            }
                        }
                        JSONObject usage = json.getJSONObject(FIELD_USAGE);
                        if (usage != null) {
                            Integer total = usage.getInt(FIELD_TOTAL_TOKENS);
                            if (total != null) tokensUsed = total;
                        }
                    } catch (Exception parseEx) {
                        log.debug("Skip unparseable stream chunk: {}", data);
                    }
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Stream AI API failed, url={}, msg={}", urlStr, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }

        long duration = System.currentTimeMillis() - start;
        log.info("AI stream done, genType={}, tokens={}, durationMs={}",
                request.getGenType(), tokensUsed, duration);

        return AIResponse.builder()
                .content(contentBuilder.toString())
                .tokensUsed(tokensUsed)
                .provider(PROVIDER_NAME)
                .durationMs(duration)
                .build();
    }

    /**
     * 从 OpenAI 兼容响应中提取生成内容。
     * 响应格式：{@code choices[0].message.content}
     */
    private String extractContent(JSONObject json) {
        JSONArray choices = json.getJSONArray(FIELD_CHOICES);
        if (choices != null && !choices.isEmpty()) {
            JSONObject firstChoice = choices.getJSONObject(0);
            if (firstChoice != null) {
                JSONObject message = firstChoice.getJSONObject(FIELD_MESSAGE);
                if (message != null) {
                    return message.getStr(FIELD_CONTENT);
                }
            }
        }
        return StrUtil.EMPTY;
    }

    /**
     * 从响应中提取 token 用量。
     */
    private int extractTokens(JSONObject json) {
        JSONObject usage = json.getJSONObject(FIELD_USAGE);
        if (usage == null) {
            return 0;
        }
        Integer total = usage.getInt(FIELD_TOTAL_TOKENS);
        return total != null ? total : 0;
    }

    // ------------------------------------------------------------------
    // Mock 模式
    // ------------------------------------------------------------------

    /**
     * 是否走 Mock 模式：mock 开启 且 （未启用真实调用 或 未配置 api-key）。
     */
    private boolean shouldUseMock() {
        return mockEnabled && (!enabled || StrUtil.isBlank(apiKey));
    }

    /**
     * 按 genType 生成结构化的模拟内容，便于联调测试。
     */
    private AIResponse generateMockResponse(AIRequest request) {
        long start = System.currentTimeMillis();
        String genType = request.getGenType();
        String content;
        int mockTokens;

        switch (genType == null ? "body" : genType) {
            case "title":
                content = "[{\"title\":\"小红书爆款标题｜这份保姆级攻略请收好\",\"score\":95},"
                        + "{\"title\":\"绝了！原来这样做才能少走弯路\",\"score\":92},"
                        + "{\"title\":\"新手小白必看｜手把手教你从0到1\",\"score\":90}]";
                mockTokens = 128;
                break;
            case "body":
                content = "姐妹们！今天分享一个超实用的内容创作方法论～\n\n"
                        + "第一步｜明确账号定位，找到差异化选题；\n"
                        + "第二步｜标题要用「数字+痛点+情绪」组合，提升点击率；\n"
                        + "第三步｜正文结构采用「总-分-总」，开头抛出钩子，中间分点输出价值，结尾引导互动；\n"
                        + "第四步｜配图要高清且信息密度高，封面是流量的关键。\n\n"
                        + "坚持复盘数据，爆款只是时间问题！#小红书运营 #内容创作";
                mockTokens = 512;
                break;
            case "tag":
                content = "[\"#小红书运营\",\"#内容创作\",\"#涨粉攻略\",\"#爆款笔记\",\"#干货分享\"]";
                mockTokens = 64;
                break;
            case "humanize":
                content = "今天给大家分享一个超实用的小技巧～亲测有效！我自己用了一个月，"
                        + "真的改变了很多。新手姐妹也不用担心，跟着步骤来就行啦～"
                        + "有什么问题欢迎在评论区问我哦！";
                mockTokens = 256;
                break;
            case "cover":
                content = "{\"coverText\":\"小红书爆款封面\",\"subtitle\":\"保姆级教程\","
                        + "\"tips\":\"建议使用3:4竖图，主标题居中放大，配色用暖色调\"}";
                mockTokens = 96;
                break;
            default:
                content = "这是一条模拟生成的AI内容，用于测试完整流程。";
                mockTokens = 128;
        }

        long duration = System.currentTimeMillis() - start + ThreadLocalRandom.current().nextInt(100, 500);
        log.info("[Mock] qwen mock generate, genType={}, tokens={}, durationMs={}",
                genType, mockTokens, duration);

        return AIResponse.builder()
                .content(content)
                .tokensUsed(mockTokens)
                .provider(PROVIDER_MOCK)
                .durationMs(duration)
                .build();
    }
}
