package com.moxi.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 调用 Python Agent 服务的请求体。
 *
 * <p>Java 侧使用 camelCase 命名，通过 {@link JsonNaming} 在序列化时
 * 自动转换为 Python 侧约定的 snake_case 字段名。</p>
 */
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AgentRequest {

    /**
     * 用户 ID。
     */
    private String userId;

    /**
     * 会话 ID，用于关联多轮交互。
     */
    private String sessionId;

    /**
     * 任务类型："generate_note" | "chat" | "refine"。
     */
    private String taskType;

    /**
     * 话题。
     */
    private String topic;

    /**
     * 博主类型。
     */
    private String bloggerType;

    /**
     * 内容风格。
     */
    private String style;

    /**
     * 期望字数。
     */
    private Integer wordCount;

    /**
     * 额外参数，透传给 Agent。
     */
    private Map<String, Object> extraParams;
}
