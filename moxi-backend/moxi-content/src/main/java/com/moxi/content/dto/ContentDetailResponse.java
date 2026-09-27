package com.moxi.content.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内容详情响应，coverImages 已从 JSON 字符串解析为 List。
 */
@Data
@NoArgsConstructor
public class ContentDetailResponse {

    private Long id;

    private Long userId;

    private String title;

    private String body;

    private String tags;

    private List<String> coverImages;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
