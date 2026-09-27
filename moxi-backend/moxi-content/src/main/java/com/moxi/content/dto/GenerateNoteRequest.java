package com.moxi.content.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 笔记生成请求。
 */
@Data
public class GenerateNoteRequest {

    @NotBlank(message = "话题不能为空")
    private String topic;

    /**
     * 风格：dry-goods（干货）/ emotional（情感）/ tutorial（教程）
     */
    private String style;

    @NotNull(message = "字数不能为空")
    @Min(value = 100, message = "字数最少100")
    @Max(value = 2000, message = "字数最多2000")
    private Integer wordCount;

    private boolean includeTags = true;

    private boolean includeCover = false;
}
