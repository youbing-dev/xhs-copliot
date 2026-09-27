package com.moxi.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Python Agent 服务同步生成接口的响应体。
 *
 * <p>Python 侧返回 snake_case 字段名，通过 {@link JsonNaming}
 * 自动映射到 Java 的 camelCase 字段。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgentResponse {

    /**
     * 候选标题列表，形如 [{"text": "...", "score": 8.5}]。
     */
    private List<Map<String, Object>> titles;

    /**
     * 正文内容。
     */
    private String body;

    /**
     * 标签列表，形如 ["#标签1", "#标签2"]。
     */
    private List<String> tags;

    /**
     * 质量评分（0~1）。
     */
    private Double qualityScore;

    /**
     * 消耗的总 token 数。
     */
    private Integer totalTokens;

    /**
     * 生成耗时（毫秒）。
     */
    private Long durationMs;

    /**
     * 反思迭代次数。
     */
    private Integer iterations;
}
