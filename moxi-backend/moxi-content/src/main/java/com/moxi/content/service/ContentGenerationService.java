package com.moxi.content.service;

import com.moxi.content.dto.GenerateNoteRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 内容生成编排服务接口。
 *
 * <p>负责串联 AI 网关、额度检查、敏感词检测和内容落库，
 * 通过 SSE 向前端实时推送生成进度。</p>
 */
public interface ContentGenerationService {

    /**
     * 生成笔记（SSE 流式输出）。
     *
     * @param userId  用户 ID
     * @param request 生成请求
     * @return SSE 事件流
     */
    SseEmitter generateNote(Long userId, GenerateNoteRequest request);
}
