package com.moxi.ai.service;

import com.moxi.ai.dto.AIResponse;

import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 生成服务接口，供上层业务模块（如内容模块）调用。
 */
public interface AIService {

    /**
     * 直接使用已渲染好的 prompt 进行生成。
     *
     * @param userId  用户 ID
     * @param genType 生成类型
     * @param prompt  已渲染的 prompt 文本
     * @return 生成响应
     */
    AIResponse generate(Long userId, String genType, String prompt);

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
    AIResponse generateWithTemplate(Long userId, Long contentId, String promptName,
                                    String genType, Map<String, String> variables);

    /**
     * 使用模板渲染后进行流式生成，每段文本回调 chunkConsumer。
     */
    AIResponse generateStreamWithTemplate(Long userId, Long contentId, String promptName,
                                          String genType, Map<String, String> variables,
                                          Consumer<String> chunkConsumer);
}
