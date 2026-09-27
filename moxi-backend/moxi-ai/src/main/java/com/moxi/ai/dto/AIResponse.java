package com.moxi.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 生成响应 DTO，由适配器返回，携带生成内容与计量信息。
 */
@Data
@Builder
public class AIResponse {

    /**
     * 生成的文本内容
     */
    private String content;

    /**
     * 本次生成消耗的 token 数
     */
    private int tokensUsed;

    /**
     * 实际提供服务的供应商标识
     */
    private String provider;

    /**
     * 生成耗时（毫秒）
     */
    private long durationMs;
}
