package com.moxi.content.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新内容请求。status 为可选字段，为空时保持原状态不变。
 */
@Data
@NoArgsConstructor
public class UpdateContentRequest {

    private String title;

    private String body;

    private String tags;

    private List<String> coverImages;

    private String status;
}
