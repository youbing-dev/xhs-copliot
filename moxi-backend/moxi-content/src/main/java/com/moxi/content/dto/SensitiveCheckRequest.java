package com.moxi.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 敏感词检测请求。
 */
@Data
public class SensitiveCheckRequest {

    @NotBlank(message = "检测内容不能为空")
    private String text;
}
