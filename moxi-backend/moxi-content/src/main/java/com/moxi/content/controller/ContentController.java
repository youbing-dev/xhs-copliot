package com.moxi.content.controller;

import com.moxi.common.response.ApiResult;
import com.moxi.content.domain.ContentVersion;
import com.moxi.content.dto.ContentDetailResponse;
import com.moxi.content.dto.ContentHistoryResponse;
import com.moxi.content.dto.CreateContentRequest;
import com.moxi.content.dto.GenerateNoteRequest;
import com.moxi.content.dto.SensitiveCheckRequest;
import com.moxi.content.dto.SensitiveCheckResponse;
import com.moxi.content.dto.UpdateContentRequest;
import com.moxi.content.service.ContentGenerationService;
import com.moxi.content.service.ContentService;
import com.moxi.content.service.SensitiveWordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 内容相关 REST 接口。
 *
 * <p>所有接口均需鉴权，userId 由上游网关/拦截器写入 request attribute，
 * 控制器直接读取使用。</p>
 */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final ContentGenerationService contentGenerationService;
    private final SensitiveWordService sensitiveWordService;

    /**
     * 分页获取当前用户的内容历史。
     */
    @GetMapping("/history")
    public ApiResult<ContentHistoryResponse> history(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(contentService.getHistory(userId, page, size));
    }

    /**
     * 获取内容详情。
     */
    @GetMapping("/{id}")
    public ApiResult<ContentDetailResponse> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(contentService.getDetail(userId, id));
    }

    /**
     * 创建新内容。
     */
    @PostMapping
    public ApiResult<ContentDetailResponse> create(@RequestBody CreateContentRequest req,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(contentService.create(userId, req));
    }

    /**
     * 更新内容。
     */
    @PutMapping("/{id}")
    public ApiResult<ContentDetailResponse> update(@PathVariable Long id,
                                                   @RequestBody UpdateContentRequest req,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(contentService.update(userId, id, req));
    }

    /**
     * 删除内容及其所有版本。
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.delete(userId, id);
        return ApiResult.success();
    }

    /**
     * 发布内容。
     */
    @PostMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.publish(userId, id);
        return ApiResult.success();
    }

    /**
     * 获取内容的所有版本快照。
     */
    @GetMapping("/{id}/versions")
    public ApiResult<List<ContentVersion>> versions(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(contentService.getVersions(userId, id));
    }

    /**
     * AI 生成笔记（SSE 流式输出）。
     * 推送 analyzing → generating → title → body → tags → done 阶段事件。
     */
    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generate(@Valid @RequestBody GenerateNoteRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return contentGenerationService.generateNote(userId, req);
    }

    /**
     * 敏感词检测。
     */
    @PostMapping("/check-sensitive")
    public ApiResult<SensitiveCheckResponse> checkSensitive(@Valid @RequestBody SensitiveCheckRequest req) {
        return ApiResult.success(sensitiveWordService.check(req.getText()));
    }
}
