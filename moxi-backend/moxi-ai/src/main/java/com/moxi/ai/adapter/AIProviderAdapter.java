package com.moxi.ai.adapter;

import com.moxi.ai.dto.AIRequest;
import com.moxi.ai.dto.AIResponse;

import java.util.function.Consumer;

/**
 * AI 供应方适配器接口。
 *
 * <p>每个具体供应方（通义千问、文心、OpenAI 等）实现本接口，
 * 由 {@code Dispatcher} 统一调度，屏蔽不同厂商 API 的差异。</p>
 */
public interface AIProviderAdapter {

    /**
     * 获取供应商标识名称，如 "qwen"。
     *
     * @return 供应商标识
     */
    String getProviderName();

    /**
     * 同步生成内容。
     *
     * @param request 生成请求
     * @return 生成响应
     */
    AIResponse generate(AIRequest request);

    /**
     * 流式生成内容，每产出一段文本回调 chunkConsumer。
     *
     * @param request       生成请求
     * @param chunkConsumer 每段文本的回调
     * @return 生成响应（含完整内容和 token 用量）
     */
    AIResponse generateStream(AIRequest request, Consumer<String> chunkConsumer);

    /**
     * 健康检查，判断当前适配器是否可用。
     *
     * @return true 表示可用
     */
    boolean isAvailable();

    /**
     * 获取当前负载水位，取值 0.0-1.0。
     *
     * @return 负载水位
     */
    float getLoad();
}
