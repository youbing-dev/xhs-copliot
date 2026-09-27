package com.moxi.content.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建内容请求。新建内容默认以 draft 状态保存。
 */
@Data
@NoArgsConstructor
public class CreateContentRequest {

    private String title;

    private String body;

    private String tags;

    private List<String> coverImages;
}
