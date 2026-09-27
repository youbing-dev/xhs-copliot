package com.moxi.content.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moxi.content.dto.CreateContentRequest;
import com.moxi.content.dto.ContentDetailResponse;
import com.moxi.content.dto.SensitiveCheckResponse;
import com.moxi.content.service.ContentService;
import com.moxi.content.service.SensitiveWordService;
import com.moxi.user.domain.UserProfile;
import com.moxi.user.repository.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内部 API —— 供 Python Agent 服务回调。
 *
 * <p>安全：所有接口通过请求头 {@code X-Internal-Secret} 校验共享密钥，
 * 密钥由配置项 {@code agent.internal-secret} 提供。该路径在 Spring Security
 * 中放行，鉴权完全依赖内部密钥。</p>
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private static final String DEFAULT_BLOGGER_TYPE = "知识干货";

    private final UserProfileMapper userProfileMapper;
    private final SensitiveWordService sensitiveWordService;
    private final ContentService contentService;

    @Value("${agent.internal-secret:moxi-internal-2024}")
    private String internalSecret;

    /**
     * 获取用户画像。查不到时返回默认值，保证 Agent 侧调用不中断。
     */
    @GetMapping("/user/{userId}/profile")
    public Map<String, Object> getUserProfile(@PathVariable Long userId,
                                              @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("bloggerType", DEFAULT_BLOGGER_TYPE);
        result.put("nickname", "");
        result.put("bio", "");
        result.put("avatarUrl", "");

        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        UserProfile profile = userProfileMapper.selectOne(wrapper);
        if (profile != null) {
            if (StrUtil.isNotBlank(profile.getBloggerType())) {
                result.put("bloggerType", profile.getBloggerType());
            }
            result.put("nickname", StrUtil.nullToEmpty(profile.getNickname()));
            result.put("bio", StrUtil.nullToEmpty(profile.getBio()));
            result.put("avatarUrl", StrUtil.nullToEmpty(profile.getAvatarUrl()));
        }
        return result;
    }

    /**
     * 敏感词校验。
     *
     * @param body 形如 {"text": "待检测文本"}
     * @return {"safe": true/false, "violations": [...], "filteredText": "..."}
     */
    @PostMapping("/content/sensitive-check")
    public Map<String, Object> sensitiveCheck(@RequestBody Map<String, String> body,
                                              @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);
        String text = body != null ? body.get("text") : null;

        Map<String, Object> result = new HashMap<>();
        if (StrUtil.isBlank(text)) {
            result.put("safe", true);
            result.put("violations", new ArrayList<String>());
            result.put("filteredText", "");
            return result;
        }

        SensitiveCheckResponse check = sensitiveWordService.check(text);
        List<String> violations = check.getSensitiveWords() != null
                ? check.getSensitiveWords() : new ArrayList<>();
        result.put("safe", !check.isHasSensitive());
        result.put("violations", violations);
        result.put("filteredText", StrUtil.nullToEmpty(check.getFilteredText()));
        return result;
    }

    /**
     * 保存草稿。
     *
     * @param body 形如 {"user_id": 1, "title": "...", "body": "...", "tags": [...] 或 "..."}
     * @return {"success": true, "contentId": 123}
     */
    @PostMapping("/content/save-draft")
    public Map<String, Object> saveDraft(@RequestBody Map<String, Object> body,
                                         @RequestHeader("X-Internal-Secret") String secret) {
        validateSecret(secret);

        Long userId = parseUserId(body);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 user_id");
        }

        CreateContentRequest req = new CreateContentRequest();
        req.setTitle(asString(body.get("title")));
        req.setBody(asString(body.get("body")));
        req.setTags(joinTags(body.get("tags")));
        req.setCoverImages(new ArrayList<>());

        ContentDetailResponse saved = contentService.create(userId, req);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("contentId", saved.getId());
        log.info("Internal save-draft success, userId={}, contentId={}", userId, saved.getId());
        return result;
    }

    // ------------------------------------------------------------------
    // 内部工具方法
    // ------------------------------------------------------------------

    private void validateSecret(String secret) {
        if (secret == null || !internalSecret.equals(secret)) {
            log.warn("Internal API rejected: invalid X-Internal-Secret");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal secret");
        }
    }

    private Long parseUserId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object raw = body.get("user_id");
        if (raw == null) {
            raw = body.get("userId");
        }
        if (raw == null) {
            return null;
        }
        try {
            return Long.valueOf(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /**
     * tags 可能是数组或逗号分隔字符串，统一转为逗号分隔字符串落库。
     */
    private String joinTags(Object tags) {
        if (tags == null) {
            return "";
        }
        if (tags instanceof Collection<?> collection) {
            List<String> parts = new ArrayList<>();
            for (Object o : collection) {
                if (o != null) {
                    parts.add(o.toString());
                }
            }
            return String.join(",", parts);
        }
        return tags.toString();
    }
}
