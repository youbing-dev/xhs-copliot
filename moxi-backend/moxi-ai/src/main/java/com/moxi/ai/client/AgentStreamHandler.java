package com.moxi.ai.client;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * 解析 Python Agent 服务返回的 SSE（Server-Sent Events）流。
 *
 * <p>SSE 报文格式：</p>
 * <pre>
 *   event: thinking
 *   data: {"step": "planning", "content": "..."}
 *
 *   event: result
 *   data: {"titles": [...], "body": "..."}
 * </pre>
 *
 * <p>解析规则：</p>
 * <ul>
 *   <li>以 {@code event:} 开头的行指定事件类型；</li>
 *   <li>以 {@code data:} 开头的行为数据体，同一个事件可包含多行 data，
 *       多行之间使用换行符拼接；</li>
 *   <li>空行表示一个事件结束，此时触发回调；</li>
 *   <li>以 {@code :} 开头的行为注释（心跳），忽略。</li>
 * </ul>
 */
@Slf4j
public class AgentStreamHandler {

    private static final String EVENT_PREFIX = "event:";
    private static final String DATA_PREFIX = "data:";
    private static final String DEFAULT_EVENT = "message";

    private AgentStreamHandler() {
    }

    /**
     * 逐行读取 SSE 流并解析事件，每收到一个完整事件即回调。
     *
     * @param reader       SSE 流读取器
     * @param eventHandler 事件回调 (eventType, jsonData) -> void
     * @throws IOException 读取流失败时抛出
     */
    public static void handle(BufferedReader reader, BiConsumer<String, String> eventHandler)
            throws IOException {
        String currentEvent = null;
        StringBuilder dataBuffer = new StringBuilder();
        boolean hasData = false;

        String line;
        while ((line = reader.readLine()) != null) {
            // 空行：一个事件结束
            if (line.isEmpty()) {
                if (hasData || currentEvent != null) {
                    dispatch(eventHandler, currentEvent, dataBuffer.toString());
                }
                currentEvent = null;
                dataBuffer.setLength(0);
                hasData = false;
                continue;
            }

            // 注释行（心跳），忽略
            if (line.startsWith(":")) {
                continue;
            }

            if (line.startsWith(EVENT_PREFIX)) {
                currentEvent = stripPrefix(line, EVENT_PREFIX);
            } else if (line.startsWith(DATA_PREFIX)) {
                if (hasData) {
                    dataBuffer.append('\n');
                }
                dataBuffer.append(stripPrefix(line, DATA_PREFIX));
                hasData = true;
            }
            // 其它字段（id:、retry:）当前无需处理，忽略
        }

        // 流结束时若仍有未派发的事件，补发一次
        if (hasData || currentEvent != null) {
            dispatch(eventHandler, currentEvent, dataBuffer.toString());
        }
    }

    private static void dispatch(BiConsumer<String, String> eventHandler, String event, String data) {
        String eventType = (event == null || event.isEmpty()) ? DEFAULT_EVENT : event;
        try {
            eventHandler.accept(eventType, data);
        } catch (Exception e) {
            log.warn("Agent SSE event handler failed, event={}, err={}", eventType, e.getMessage());
        }
    }

    /**
     * 去除字段前缀，并吞掉冒号后可选的一个空格。
     */
    private static String stripPrefix(String line, String prefix) {
        String value = line.substring(prefix.length());
        if (value.startsWith(" ")) {
            value = value.substring(1);
        }
        return value;
    }
}
