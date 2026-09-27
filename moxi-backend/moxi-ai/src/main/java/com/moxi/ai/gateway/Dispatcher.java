package com.moxi.ai.gateway;

import com.moxi.ai.adapter.AIProviderAdapter;
import com.moxi.ai.dto.AIRequest;
import com.moxi.ai.dto.AIResponse;
import com.moxi.common.exception.BusinessException;
import com.moxi.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI 请求调度器。
 *
 * <p>Spring 会将所有 {@link AIProviderAdapter} 实现注入到列表中，
 * 调度时按顺序选取第一个 {@link AIProviderAdapter#isAvailable()} 为 true 的适配器进行下发。
 * 若无可用适配器，抛出 {@link ErrorCode#AI_SERVICE_UNAVAILABLE}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Dispatcher {

    private final List<AIProviderAdapter> adapters;

    /**
     * 下发生成请求至首个可用适配器。
     *
     * @param request 生成请求
     * @return 生成响应
     * @throws BusinessException 当无可用适配器时抛出
     */
    public AIResponse dispatch(AIRequest request) {
        AIProviderAdapter provider = getProvider();
        log.info("Dispatch AI request, genType={}, provider={}", request.getGenType(), provider.getProviderName());
        return provider.generate(request);
    }

    /**
     * 下发流式生成请求至首个可用适配器。
     */
    public AIResponse dispatchStream(AIRequest request, Consumer<String> chunkConsumer) {
        AIProviderAdapter provider = getProvider();
        log.info("Dispatch AI stream request, genType={}, provider={}", request.getGenType(), provider.getProviderName());
        return provider.generateStream(request, chunkConsumer);
    }

    /**
     * 获取首个可用的供应方适配器。
     *
     * @return 可用适配器
     * @throws BusinessException 当无可用适配器时抛出
     */
    public AIProviderAdapter getProvider() {
        if (adapters == null || adapters.isEmpty()) {
            log.error("No AI provider adapter registered");
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
        for (AIProviderAdapter adapter : adapters) {
            if (adapter.isAvailable()) {
                return adapter;
            }
        }
        log.error("All AI provider adapters unavailable, count={}", adapters.size());
        throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }
}
