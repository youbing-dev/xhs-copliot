package com.moxi.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * AI 生成请求 DTO，由网关层组装后下发给适配器。
 */
@Data
@Builder
public class AIRequest {

    /**
     * 已渲染完成的 prompt 文本
     */
    private String prompt;

    /**
     * 模型名称，如 "qwen-turbo"
     */
    private String model;

    /**
     * 最大生成 token 数
     */
    private Integer maxTokens;

    /**
     * 采样温度 0.0-1.0
     */
    private Double temperature;

    /**
     * 是否使用流式模式
     */
    private boolean stream;

    /**
     * 生成类型: "title"/"body"/"tag"/"cover"/"humanize"
     */
    private String genType;
}
