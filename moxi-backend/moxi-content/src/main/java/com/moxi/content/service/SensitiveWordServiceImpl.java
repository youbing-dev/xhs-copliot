package com.moxi.content.service;

import cn.hutool.core.util.StrUtil;
import com.moxi.content.dto.SensitiveCheckResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 敏感词检测服务实现，基于 DFA（确定有限自动机）算法。
 *
 * <p>初始化时构建敏感词词典树（嵌套 HashMap），检测时逐字符遍历文本，
 * 在 O(n) 时间内完成全文匹配。</p>
 *
 * <p>内置一组常见违规词用于演示；生产环境可从数据库或文件加载。</p>
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private static final String END_KEY = "isEnd";
    private static final String END_VALUE = "1";
    private static final String REPLACEMENT = "***";

    /**
     * DFA 词典树根节点。
     */
    private final Map<String, Object> dictionary = new HashMap<>();

    /**
     * 内置敏感词库（演示用，生产环境应从外部加载）。
     */
    private static final String[] DEFAULT_WORDS = {
            "赌博", "色情", "诈骗", "刷单", "代开", "假证", "假发票",
            "违禁品", "枪支", "弹药", "毒品", "走私",
            "传销", "非法集资", "洗钱",
            "黑客", "木马", "病毒程序",
            "代孕", "买卖器官",
            "刷量", "买粉", "卖粉",
            " censorship", "vpn服务"
    };

    @PostConstruct
    void init() {
        int count = 0;
        for (String word : DEFAULT_WORDS) {
            if (StrUtil.isNotBlank(word)) {
                addWord(word);
                count++;
            }
        }
        log.info("Sensitive word dictionary loaded, size={}", count);
    }

    /**
     * 向 DFA 词典树中添加一个敏感词。
     */
    @SuppressWarnings("unchecked")
    private void addWord(String word) {
        Map<String, Object> current = dictionary;
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            Map<String, Object> next = (Map<String, Object>) current.get(ch);
            if (next == null) {
                next = new HashMap<>();
                current.put(ch, next);
            }
            current = next;
            if (i == word.length() - 1) {
                current.put(END_KEY, END_VALUE);
            }
        }
    }

    @Override
    public List<String> detect(String text) {
        if (StrUtil.isBlank(text)) {
            return List.of();
        }
        Set<String> found = new LinkedHashSet<>();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            int end = checkMatch(text, i);
            if (end > i) {
                found.add(text.substring(i, end));
                i = end - 1; // 跳过已匹配的部分
            }
        }
        return new ArrayList<>(found);
    }

    @Override
    public SensitiveCheckResponse check(String text) {
        List<String> words = detect(text);
        SensitiveCheckResponse resp = new SensitiveCheckResponse();
        resp.setHasSensitive(!words.isEmpty());
        resp.setSensitiveWords(words);
        resp.setCount(words.size());
        resp.setFilteredText(filter(text));
        return resp;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String filter(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        int length = text.length();
        int i = 0;

        while (i < length) {
            int end = checkMatch(text, i);
            if (end > i) {
                result.append(REPLACEMENT);
                i = end;
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    /**
     * 从指定位置开始，在 DFA 词典树中匹配最长敏感词。
     *
     * @param text  原始文本
     * @param start 起始索引
     * @return 匹配结束索引（不包含），未匹配返回 start
     */
    @SuppressWarnings("unchecked")
    private int checkMatch(String text, int start) {
        Map<String, Object> current = dictionary;
        int end = start;
        int length = text.length();

        for (int i = start; i < length; i++) {
            String ch = String.valueOf(text.charAt(i));
            Object next = current.get(ch);
            if (next == null) {
                break;
            }
            current = (Map<String, Object>) next;
            if (END_VALUE.equals(current.get(END_KEY))) {
                end = i + 1; // 记录最长匹配
            }
        }
        return end;
    }
}
