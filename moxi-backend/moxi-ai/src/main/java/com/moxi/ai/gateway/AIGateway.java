package com.moxi.ai.gateway;

import com.moxi.ai.domain.AiPrompt;
import com.moxi.ai.domain.GenerationRecord;
import com.moxi.ai.dto.AIRequest;
import com.moxi.ai.dto.AIResponse;
import com.moxi.ai.meter.TokenMeter;
import com.moxi.ai.prompt.PromptManager;
import com.moxi.ai.repository.GenerationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 生成网关，对上层提供统一的高阶生成入口。
 *
 * <p>职责串联四件事：
 * <ol>
 *   <li>组装 {@link AIRequest} 并经 {@link Dispatcher} 下发；</li>
 *   <li>通过 {@link TokenMeter} 记录 token 用量到 Redis；</li>
 *   <li>将生成记录落库到 generation_records；</li>
 *   <li>返回 {@link AIResponse}。</li>
 * </ol>
 * </p>
 *
 * <p>{@link #generateWithTemplate(Long, Long, String, String, Map)} 在此基础上先经
 * {@link PromptManager} 渲染模板，并将 contentId 与 aiPromptId 一并写入记录。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIGateway {

    private final Dispatcher dispatcher;
    private final PromptManager promptManager;
    private final TokenMeter tokenMeter;
    private final GenerationRecordMapper generationRecordMapper;

    /**
     * 直接使用已渲染好的 prompt 进行生成。
     *
     * @param userId  用户 ID
     * @param genType 生成类型
     * @param prompt  已渲染的 prompt 文本
     * @return 生成响应
     */
    public AIResponse generate(Long userId, String genType, String prompt) {
        return executeGeneration(userId, null, null, genType, prompt, null);
    }

    /**
     * 使用指定模板渲染变量后进行生成，并关联内容与模板。
     *
     * @param userId     用户 ID
     * @param contentId  关联内容 ID
     * @param promptName 模板名称
     * @param genType    生成类型
     * @param variables  模板变量
     * @return 生成响应
     */
    public AIResponse generateWithTemplate(Long userId, Long contentId, String promptName,
                                           String genType, Map<String, String> variables) {
        // 1. 取模板实体（含 id），并渲染变量
        AiPrompt template = promptManager.getTemplate(promptName);
        String renderedPrompt = promptManager.renderTemplate(promptName, variables);
        // 2. 复用核心生成流程，并带上 contentId 与 aiPromptId
        return executeGeneration(userId, contentId, template.getId(), genType, renderedPrompt, null);
    }

    /**
     * 使用模板渲染后进行流式生成，每段文本回调 chunkConsumer。
     */
    public AIResponse generateStreamWithTemplate(Long userId, Long contentId, String promptName,
                                                  String genType, Map<String, String> variables,
                                                  Consumer<String> chunkConsumer) {
        AiPrompt template = promptManager.getTemplate(promptName);
        String renderedPrompt = promptManager.renderTemplate(promptName, variables);

        AIRequest request = AIRequest.builder()
                .prompt(renderedPrompt)
                .genType(genType)
                .stream(true)
                .build();

        AIResponse response = dispatcher.dispatchStream(request, chunkConsumer);

        // 记录 token 用量
        try {
            tokenMeter.recordUsage(userId, response.getTokensUsed());
        } catch (Exception e) {
            log.warn("Record token usage failed, userId={}, msg={}", userId, e.getMessage());
        }

        // 落库生成记录
        try {
            GenerationRecord record = new GenerationRecord();
            record.setUserId(userId);
            record.setContentId(contentId);
            record.setAiPromptId(template.getId());
            record.setGenType(genType);
            record.setAiProvider(response.getProvider());
            record.setTokensUsed(response.getTokensUsed());
            record.setDurationMs((int) response.getDurationMs());
            generationRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("Save generation record failed, userId={}, genType={}, msg={}",
                    userId, genType, e.getMessage());
        }

        log.info("AI stream generation done, userId={}, genType={}, tokens={}, durationMs={}",
                userId, genType, response.getTokensUsed(), response.getDurationMs());
        return response;
    }

    // ------------------------------------------------------------------
    // 内部核心流程
    // ------------------------------------------------------------------

    /**
     * 生成核心流程：下发 -> 计量 -> 落库 -> 返回。
     *
     * <p>统一的生成逻辑被抽取到此方法，{@link #generate} 与
     * {@link #generateWithTemplate} 均复用，保证计费、落库行为一致。</p>
     *
     * @param userId     用户 ID
     * @param contentId  关联内容 ID，可为空
     * @param aiPromptId 使用的模板 ID，可为空
     * @param genType    生成类型
     * @param prompt     已渲染的 prompt 文本
     * @param topic      选题，可为空
     * @return 生成响应
     */
    private AIResponse executeGeneration(Long userId, Long contentId, Long aiPromptId,
                                         String genType, String prompt, String topic) {
        AIRequest request = AIRequest.builder()
                .prompt(prompt)
                .genType(genType)
                .build();

        AIResponse response = dispatcher.dispatch(request);

        // 记录 token 用量
        try {
            tokenMeter.recordUsage(userId, response.getTokensUsed());
        } catch (Exception e) {
            log.warn("Record token usage failed, userId={}, msg={}", userId, e.getMessage());
        }

        // 落库生成记录
        try {
            GenerationRecord record = new GenerationRecord();
            record.setUserId(userId);
            record.setContentId(contentId);
            record.setAiPromptId(aiPromptId);
            record.setTopic(topic);
            record.setGenType(genType);
            record.setAiProvider(response.getProvider());
            record.setTokensUsed(response.getTokensUsed());
            record.setDurationMs((int) response.getDurationMs());
            generationRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("Save generation record failed, userId={}, genType={}, msg={}",
                    userId, genType, e.getMessage());
        }

        log.info("AI generation done, userId={}, genType={}, tokens={}, durationMs={}",
                userId, genType, response.getTokensUsed(), response.getDurationMs());
        return response;
    }
}
