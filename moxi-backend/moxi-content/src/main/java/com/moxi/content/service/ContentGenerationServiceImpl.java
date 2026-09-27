package com.moxi.content.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moxi.ai.client.AgentClient;
import com.moxi.ai.client.AgentRequest;
import com.moxi.ai.dto.AIResponse;
import com.moxi.ai.service.AIService;
import com.moxi.billing.domain.Membership;
import com.moxi.billing.domain.MembershipPlan;
import com.moxi.billing.repository.MembershipMapper;
import com.moxi.billing.repository.MembershipPlanMapper;
import com.moxi.common.constant.RedisKeys;
import com.moxi.content.domain.Content;
import com.moxi.content.domain.ContentVersion;
import com.moxi.content.dto.GenerateNoteRequest;
import com.moxi.content.repository.ContentMapper;
import com.moxi.content.repository.ContentVersionMapper;
import com.moxi.user.domain.UserProfile;
import com.moxi.user.repository.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 内容生成编排服务实现。
 *
 * <p>串联 AI 网关、额度检查、敏感词检测和内容落库，
 * 通过 SSE 向前端实时推送生成进度。</p>
 *
 * <p>生成流程：
 * <ol>
 *   <li>检查 Redis 日额度</li>
 *   <li>查询用户博主类型与趋势上下文</li>
 *   <li>调用 AI 生成标题 → SSE title</li>
 *   <li>调用 AI 生成正文 → SSE body</li>
 *   <li>调用 AI 生成标签 → SSE tags</li>
 *   <li>敏感词检测 → SSE warning（如有）</li>
 *   <li>保存内容 + 版本快照</li>
 *   <li>更新 Redis 额度计数</li>
 *   <li>SSE done</li>
 * </ol>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentGenerationServiceImpl implements ContentGenerationService {

    private static final long SSE_TIMEOUT = 300_000L; // 5 分钟（推理模型调用较慢）
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String QUOTA_FIELD = "notes_generated";
    private static final String STATUS_DRAFT = "draft";
    private static final String BLOGGER_TYPE_DEFAULT = "知识干货";

    private final AIService aiService;
    private final ContentMapper contentMapper;
    private final ContentVersionMapper contentVersionMapper;
    private final UserProfileMapper userProfileMapper;
    private final MembershipMapper membershipMapper;
    private final MembershipPlanMapper membershipPlanMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final SensitiveWordService sensitiveWordService;
    private final AgentClient agentClient;

    /**
     * Agent 链路模式：{@code python} 走 Python Agent 服务，{@code java} 走原有 AIGateway。
     */
    @Value("${agent.mode:java}")
    private String agentMode;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public SseEmitter generateNote(Long userId, GenerateNoteRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitter.onCompletion(() -> log.debug("SSE completed, userId={}", userId));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout, userId={}", userId);
            emitter.complete();
        });
        emitter.onError(ex -> log.error("SSE error, userId={}", userId, ex));

        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            try {
                doGenerate(userId, request, emitter, startTime);
            } catch (Exception e) {
                log.error("Generate note failed, userId={}", userId, e);
                sendErrorEvent(emitter, "生成失败：" + e.getMessage());
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }

    // ------------------------------------------------------------------
    // 核心生成流程
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void doGenerate(Long userId, GenerateNoteRequest req,
                            SseEmitter emitter, long startTime) throws IOException {
        // 0. 链路分流：agent.mode=python 时改走 Python Agent 服务，其余保持原有 AIGateway 逻辑
        if ("python".equalsIgnoreCase(agentMode)) {
            generateViaAgent(emitter, req, userId);
            return;
        }

        // 1. 检查额度
        int todayCount = getTodayGenerationCount(userId);
        MembershipPlan plan = getUserPlan(userId);
        if (!Boolean.TRUE.equals(plan.getUnlimitedNotes())
                && todayCount >= (plan.getDailyNoteQuota() != null ? plan.getDailyNoteQuota() : 3)) {
            sendEvent(emitter, "error",
                    Map.of("message", "今日生成额度已用完，升级会员可解锁更多次数"));
            return;
        }

        // 2. 查询用户博主类型
        String bloggerType = getBloggerType(userId);

        // 3. SSE: analyzing
        sendEvent(emitter, "analyzing",
                Map.of("stage", "analyzing", "message", "正在分析热门数据..."));

        // 4. 生成标题
        sendEvent(emitter, "generating",
                Map.of("stage", "generating", "message", "正在生成标题..."));

        Map<String, String> titleVars = new HashMap<>();
        titleVars.put("topic", req.getTopic());
        titleVars.put("bloggerType", bloggerType);
        AIResponse titleResp = aiService.generateWithTemplate(
                userId, null, "title-generation-v1", "title", titleVars);

        String title = parseTitle(titleResp.getContent());
        int titleScore = parseTitleScore(titleResp.getContent());
        sendEvent(emitter, "title",
                Map.of("title", title, "score", titleScore));

        // 5. 生成正文（流式）
        sendEvent(emitter, "generating",
                Map.of("stage", "generating", "message", "正在生成正文..."));

        Map<String, String> bodyVars = new HashMap<>();
        bodyVars.put("bloggerType", bloggerType);
        bodyVars.put("wordCount", String.valueOf(req.getWordCount()));
        bodyVars.put("topic", req.getTopic());
        bodyVars.put("titlePattern", title);
        bodyVars.put("tags", "");
        bodyVars.put("trendContext", "暂无趋势数据");

        StringBuilder bodyBuilder = new StringBuilder();
        Consumer<String> chunkConsumer = chunk -> {
            bodyBuilder.append(chunk);
            try {
                sendEvent(emitter, "body-chunk", Map.of("chunk", chunk));
            } catch (IOException e) {
                log.warn("Failed to send body-chunk: {}", e.getMessage());
            }
        };
        AIResponse bodyResp = aiService.generateStreamWithTemplate(
                userId, null, "note-generation-v1", "body", bodyVars, chunkConsumer);
        String body = bodyResp.getContent();
        // 发送 body 完成事件，让前端切换到下一步状态
        sendEvent(emitter, "body", Map.of("body", body));

        // 6. 生成标签
        List<String> tags = new ArrayList<>();
        if (req.isIncludeTags()) {
            sendEvent(emitter, "generating",
                    Map.of("stage", "generating", "message", "正在生成标签..."));

            Map<String, String> tagVars = new HashMap<>();
            tagVars.put("title", title);
            tagVars.put("body", body);
            tagVars.put("bloggerType", bloggerType);
            AIResponse tagResp = aiService.generateWithTemplate(
                    userId, null, "tag-generation-v1", "tag", tagVars);
            tags = parseTags(tagResp.getContent());
            sendEvent(emitter, "tags", Map.of("tags", tags));
        }

        // 7. 敏感词检测
        String fullText = title + " " + body;
        List<String> sensitiveWords = sensitiveWordService.detect(fullText);
        if (!sensitiveWords.isEmpty()) {
            sendEvent(emitter, "warning",
                    Map.of("message", "检测到敏感词", "words", sensitiveWords));
        }

        // 8. 保存内容
        Content content = saveContent(userId, title, body, tags);
        log.info("Generated and saved content id={} for userId={}", content.getId(), userId);

        // 9. 更新额度
        incrementQuota(userId);

        // 10. SSE: done
        long duration = System.currentTimeMillis() - startTime;
        sendEvent(emitter, "done",
                Map.of("contentId", content.getId(), "durationMs", duration));
    }

    // ------------------------------------------------------------------
    // Python Agent 链路
    // ------------------------------------------------------------------

    /**
     * 通过 Python Agent 服务生成笔记（SSE 流式）。
     *
     * <p>将 Agent 返回的 SSE 事件映射为前端事件：</p>
     * <ul>
     *   <li>{@code thinking} / {@code tool_start} / {@code tool_result} → {@code status}</li>
     *   <li>{@code plan} → {@code plan}</li>
     *   <li>{@code content_chunk} → {@code body-chunk}</li>
     *   <li>{@code result} → {@code result}（含 titles + body + tags，并落库、扣额度）</li>
     *   <li>{@code done} → {@code done}</li>
     *   <li>{@code error} → {@code error}</li>
     * </ul>
     *
     * @param emitter SSE 发射器
     * @param req     生成请求
     * @param userId  用户 ID
     */
    @SuppressWarnings("unchecked")
    private void generateViaAgent(SseEmitter emitter, GenerateNoteRequest req, Long userId) throws IOException {
        long startTime = System.currentTimeMillis();

        // 额度检查（与 java 链路保持一致）
        int todayCount = getTodayGenerationCount(userId);
        MembershipPlan plan = getUserPlan(userId);
        if (!Boolean.TRUE.equals(plan.getUnlimitedNotes())
                && todayCount >= (plan.getDailyNoteQuota() != null ? plan.getDailyNoteQuota() : 3)) {
            sendEvent(emitter, "error",
                    Map.of("message", "今日生成额度已用完，升级会员可解锁更多次数"));
            return;
        }

        String bloggerType = getBloggerType(userId);

        AgentRequest agentRequest = AgentRequest.builder()
                .userId(String.valueOf(userId))
                .sessionId(UUID.randomUUID().toString())
                .taskType("generate_note")
                .topic(req.getTopic())
                .bloggerType(bloggerType)
                .style(req.getStyle())
                .wordCount(req.getWordCount())
                .extraParams(new HashMap<>())
                .build();

        // 跨回调累积的结果状态（lambda 内需 effectively-final）
        final StringBuilder bodyAccumulator = new StringBuilder();
        final String[] titleHolder = {null};
        final Long[] contentIdHolder = {null};

        BiConsumer<String, String> handler = (eventType, data) -> {
            try {
                JSONObject obj = StrUtil.isBlank(data) ? new JSONObject() : JSONUtil.parseObj(data);
                switch (eventType) {
                    case "thinking":
                        sendEvent(emitter, "status", Map.of(
                                "step", obj.getStr("step", ""),
                                "message", obj.getStr("content", "")));
                        break;
                    case "plan":
                        sendEvent(emitter, "plan", Map.of(
                                "steps", toList(obj.getJSONArray("steps"))));
                        break;
                    case "tool_start":
                        sendEvent(emitter, "status", Map.of(
                                "step", "tool_start",
                                "tool", obj.getStr("tool", ""),
                                "message", "正在执行：" + obj.getStr("tool", "")));
                        break;
                    case "tool_result":
                        sendEvent(emitter, "status", Map.of(
                                "step", "tool_result",
                                "tool", obj.getStr("tool", ""),
                                "message", obj.getStr("result_summary", "")));
                        break;
                    case "content_chunk": {
                        String field = obj.getStr("field", "body");
                        String chunk = obj.getStr("chunk", "");
                        if ("body".equals(field)) {
                            bodyAccumulator.append(chunk);
                        }
                        sendEvent(emitter, "body-chunk", Map.of("chunk", chunk, "field", field));
                        break;
                    }
                    case "result": {
                        JSONArray titles = obj.getJSONArray("titles");
                        String title = parseAgentTitle(titles);
                        String body = obj.getStr("body", bodyAccumulator.toString());
                        List<String> tags = toStringList(obj.getJSONArray("tags"));
                        titleHolder[0] = title;

                        Map<String, Object> resultPayload = new HashMap<>();
                        resultPayload.put("titles", toList(titles));
                        resultPayload.put("body", body);
                        resultPayload.put("tags", tags);
                        if (obj.get("quality_score") != null) {
                            resultPayload.put("qualityScore", obj.get("quality_score"));
                        }
                        sendEvent(emitter, "result", resultPayload);

                        // 敏感词检测
                        List<String> sensitiveWords = sensitiveWordService.detect(title + " " + body);
                        if (!sensitiveWords.isEmpty()) {
                            sendEvent(emitter, "warning",
                                    Map.of("message", "检测到敏感词", "words", sensitiveWords));
                        }

                        // 落库 + 扣额度
                        Content content = saveContent(userId, title, body, tags);
                        contentIdHolder[0] = content.getId();
                        incrementQuota(userId);
                        log.info("Agent generated and saved content id={} for userId={}",
                                content.getId(), userId);
                        break;
                    }
                    case "done": {
                        long duration = System.currentTimeMillis() - startTime;
                        Long agentDuration = obj.getLong("duration_ms");
                        Map<String, Object> donePayload = new HashMap<>();
                        if (contentIdHolder[0] != null) {
                            donePayload.put("contentId", contentIdHolder[0]);
                        }
                        donePayload.put("durationMs", agentDuration != null ? agentDuration : duration);
                        donePayload.put("status", obj.getStr("status", "completed"));
                        sendEvent(emitter, "done", donePayload);
                        break;
                    }
                    case "error":
                        sendEvent(emitter, "error", Map.of(
                                "code", obj.getStr("code", "AGENT_ERROR"),
                                "message", obj.getStr("message", "Agent 生成失败")));
                        break;
                    default:
                        log.debug("Unhandled agent event type={}", eventType);
                        break;
                }
            } catch (Exception e) {
                log.warn("Handle agent event failed, type={}, err={}", eventType, e.getMessage());
            }
        };

        agentClient.generateStream(agentRequest, handler);

        // 兜底：Agent 未回传 result 但已流式输出正文时，仍保存并结束
        if (contentIdHolder[0] == null && StrUtil.isNotBlank(bodyAccumulator.toString())) {
            Content content = saveContent(userId, "未生成标题", bodyAccumulator.toString(), new ArrayList<>());
            incrementQuota(userId);
            long duration = System.currentTimeMillis() - startTime;
            sendEvent(emitter, "done", Map.of("contentId", content.getId(), "durationMs", duration));
        }
    }

    /**
     * 从 Agent 返回的 titles 数组中取第一个标题文本。
     * 元素形如 {"text": "...", "score": 8.5}。
     */
    private String parseAgentTitle(JSONArray titles) {
        if (titles != null && !titles.isEmpty()) {
            JSONObject first = titles.getJSONObject(0);
            if (first != null) {
                String text = first.getStr("text");
                if (StrUtil.isBlank(text)) {
                    text = first.getStr("title");
                }
                if (StrUtil.isNotBlank(text)) {
                    return text;
                }
            }
        }
        return "未生成标题";
    }

    /**
     * 将 hutool JSONArray 转为字符串列表。
     */
    private List<String> toStringList(JSONArray arr) {
        List<String> list = new ArrayList<>();
        if (arr != null) {
            for (Object o : arr) {
                if (o != null) {
                    list.add(o.toString());
                }
            }
        }
        return list;
    }

    /**
     * 将 hutool JSONArray 转为普通 List，便于 Jackson 序列化后推送前端。
     */
    private List<Object> toList(JSONArray arr) {
        List<Object> list = new ArrayList<>();
        if (arr != null) {
            list.addAll(arr);
        }
        return list;
    }

    // ------------------------------------------------------------------
    // 辅助方法
    // ------------------------------------------------------------------

    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private void sendErrorEvent(SseEmitter emitter, String message) {
        try {
            sendEvent(emitter, "error", Map.of("message", message));
        } catch (IOException e) {
            log.warn("Failed to send SSE error event: {}", e.getMessage());
        }
    }

    /**
     * 去除 AI 返回内容中的 markdown 代码块包裹。
     * LongCat 等推理模型常将 JSON 包裹在 ```json ... ``` 中。
     */
    private String stripMarkdownCodeBlock(String content) {
        if (StrUtil.isBlank(content)) {
            return content;
        }
        String trimmed = content.trim();
        // 去除开头的 ```json 或 ``` 包裹
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            // 去除结尾的 ```
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    /**
     * 从 AI 返回的 JSON 数组中解析第一个标题。
     */
    private String parseTitle(String aiContent) {
        String cleaned = stripMarkdownCodeBlock(aiContent);
        try {
            JSONArray arr = JSONUtil.parseArray(cleaned);
            if (!arr.isEmpty()) {
                JSONObject first = arr.getJSONObject(0);
                if (first != null) {
                    return first.getStr("title", cleaned);
                }
            }
        } catch (Exception e) {
            log.warn("Parse title failed, using raw content: {}", e.getMessage());
        }
        return StrUtil.isBlank(cleaned) ? "未生成标题" : StrUtil.sub(cleaned, 0, 200);
    }

    /**
     * 从 AI 返回的 JSON 数组中解析第一个标题评分。
     */
    private int parseTitleScore(String aiContent) {
        String cleaned = stripMarkdownCodeBlock(aiContent);
        try {
            JSONArray arr = JSONUtil.parseArray(cleaned);
            if (!arr.isEmpty()) {
                JSONObject first = arr.getJSONObject(0);
                if (first != null) {
                    Integer score = first.getInt("score");
                    return score != null ? score : 80;
                }
            }
        } catch (Exception e) {
            log.warn("Parse title score failed: {}", e.getMessage());
        }
        return 80;
    }

    /**
     * 从 AI 返回的 JSON 数组中解析标签列表。
     */
    private List<String> parseTags(String aiContent) {
        String cleaned = stripMarkdownCodeBlock(aiContent);
        try {
            List<String> tags = JSONUtil.toList(cleaned, String.class);
            return tags != null ? tags : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Parse tags failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户的博主类型。
     */
    private String getBloggerType(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile profile = userProfileMapper.selectOne(wrapper);
        if (profile != null && StrUtil.isNotBlank(profile.getBloggerType())) {
            return profile.getBloggerType();
        }
        return BLOGGER_TYPE_DEFAULT;
    }

    /**
     * 获取用户当前会员计划。
     * 无活跃会员时返回 free 计划。
     */
    private MembershipPlan getUserPlan(Long userId) {
        // 查询活跃会员
        LambdaQueryWrapper<Membership> mWrapper = new LambdaQueryWrapper<>();
        mWrapper.eq(Membership::getUserId, userId)
                .eq(Membership::getStatus, "active")
                .last("LIMIT 1");
        Membership membership = membershipMapper.selectOne(mWrapper);

        Long planId;
        if (membership != null) {
            planId = membership.getPlanId();
        } else {
            planId = 1L; // 默认 free 计划
        }

        MembershipPlan plan = membershipPlanMapper.selectById(planId);
        if (plan == null) {
            plan = membershipPlanMapper.selectById(1L);
        }
        if (plan == null) {
            // 兜底：构造一个 free 级别的计划
            plan = new MembershipPlan();
            plan.setId(1L);
            plan.setName("free");
            plan.setDailyNoteQuota(3);
            plan.setUnlimitedNotes(false);
        }
        return plan;
    }

    /**
     * 从 Redis 获取今日已生成次数。
     */
    private int getTodayGenerationCount(Long userId) {
        String date = LocalDate.now().format(DATE_FMT);
        String key = RedisKeys.quotaUser(userId, date);
        Object val = stringRedisTemplate.opsForHash().get(key, QUOTA_FIELD);
        if (val == null) {
            return 0;
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 在 Redis 中递增今日生成次数。
     */
    private void incrementQuota(Long userId) {
        String date = LocalDate.now().format(DATE_FMT);
        String key = RedisKeys.quotaUser(userId, date);
        stringRedisTemplate.opsForHash().increment(key, QUOTA_FIELD, 1);
        // 设置过期时间（48小时，确保跨天后自动清理）
        stringRedisTemplate.expire(key, java.time.Duration.ofHours(48));
    }

    /**
     * 保存生成内容到数据库，并创建版本快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public Content saveContent(Long userId, String title, String body, List<String> tags) {
        Content content = new Content();
        content.setUserId(userId);
        content.setTitle(title);
        content.setBody(body);
        content.setTags(String.join(",", tags));
        content.setCoverImages("[]");
        content.setStatus(STATUS_DRAFT);
        contentMapper.insert(content);

        // 保存版本快照
        ContentVersion version = new ContentVersion();
        version.setContentId(content.getId());
        version.setVersionNum(1);
        version.setTitle(title);
        version.setBody(body);
        version.setTags(content.getTags());
        version.setCoverImages("[]");
        contentVersionMapper.insert(version);

        return content;
    }
}
