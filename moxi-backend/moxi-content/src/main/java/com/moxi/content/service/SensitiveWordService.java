package com.moxi.content.service;

import com.moxi.content.dto.SensitiveCheckResponse;

import java.util.List;

/**
 * 敏感词检测服务接口。
 * 基于 DFA（确定有限自动机）算法实现高效敏感词匹配。
 */
public interface SensitiveWordService {

    /**
     * 检测文本中的敏感词。
     *
     * @param text 待检测文本
     * @return 命中的敏感词列表（去重）
     */
    List<String> detect(String text);

    /**
     * 检测文本并返回完整响应（含过滤后文本）。
     */
    SensitiveCheckResponse check(String text);

    /**
     * 过滤文本，将敏感词替换为 ***。
     */
    String filter(String text);
}
