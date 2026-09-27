package com.moxi.content.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容历史列表响应。
 */
@Data
@NoArgsConstructor
public class ContentHistoryResponse {

    private List<ContentItem> items;

    private long total;

    private int page;

    private int size;

    /**
     * 历史列表中的单条内容摘要。
     */
    @Data
    @NoArgsConstructor
    public static class ContentItem {

        private Long id;

        private String title;

        private String status;

        private LocalDateTime createdAt;
    }
}
