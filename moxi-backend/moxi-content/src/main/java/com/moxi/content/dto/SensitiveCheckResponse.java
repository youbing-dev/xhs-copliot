package com.moxi.content.dto;

import lombok.Data;

import java.util.List;

/**
 * 敏感词检测响应。
 */
@Data
public class SensitiveCheckResponse {

    private boolean hasSensitive;

    private List<String> sensitiveWords;

    private String filteredText;

    private int count;
}
