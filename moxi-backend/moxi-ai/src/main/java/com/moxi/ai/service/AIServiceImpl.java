package com.moxi.ai.service;

import com.moxi.ai.dto.AIResponse;
import com.moxi.ai.gateway.AIGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

/**
 * AI 生成服务实现，委托给 {@link AIGateway} 完成实际生成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AIGateway aiGateway;

    @Override
    public AIResponse generate(Long userId, String genType, String prompt) {
        return aiGateway.generate(userId, genType, prompt);
    }

    @Override
    public AIResponse generateWithTemplate(Long userId, Long contentId, String promptName,
                                           String genType, Map<String, String> variables) {
        return aiGateway.generateWithTemplate(userId, contentId, promptName, genType, variables);
    }

    @Override
    public AIResponse generateStreamWithTemplate(Long userId, Long contentId, String promptName,
                                                  String genType, Map<String, String> variables,
                                                  Consumer<String> chunkConsumer) {
        return aiGateway.generateStreamWithTemplate(userId, contentId, promptName, genType, variables, chunkConsumer);
    }
}
