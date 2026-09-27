package com.moxi.ai.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moxi.ai.domain.AiPrompt;
import com.moxi.ai.repository.AiPromptMapper;
import com.moxi.common.exception.BusinessException;
import com.moxi.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Prompt 模板管理器。
 *
 * <p>负责从 {@code ai_prompts} 表加载启用的模板并完成变量渲染。
 * 取用规则：按 name 查询 enabled=1 的记录，按版本号倒序取最新一条。</p>
 *
 * <p>变量占位符格式为 {@code {{variableName}}}，渲染时逐项做字符串替换。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptManager {

    private final AiPromptMapper aiPromptMapper;

    /**
     * 加载指定名称的最新启用模板，并完成变量渲染。
     *
     * @param name      模板名称
     * @param variables 变量键值对，可为空
     * @return 渲染后的模板字符串
     * @throws BusinessException 模板不存在时抛出 {@link ErrorCode#PROMPT_NOT_FOUND}
     */
    public String renderTemplate(String name, Map<String, String> variables) {
        AiPrompt prompt = getTemplate(name);
        String template = prompt.getTemplate();
        if (template == null) {
            return "";
        }
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                template = template.replace(placeholder, value);
            }
        }
        log.debug("Rendered prompt name={}, version={}", name, prompt.getVersion());
        return template;
    }

    /**
     * 获取指定名称的最新启用模板实体。
     *
     * @param name 模板名称
     * @return 模板实体
     * @throws BusinessException 模板不存在时抛出 {@link ErrorCode#PROMPT_NOT_FOUND}
     */
    public AiPrompt getTemplate(String name) {
        LambdaQueryWrapper<AiPrompt> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiPrompt::getName, name)
                .eq(AiPrompt::getEnabled, true)
                .orderByDesc(AiPrompt::getVersion)
                .last("LIMIT 1");
        AiPrompt prompt = aiPromptMapper.selectOne(wrapper);
        if (prompt == null) {
            log.warn("Prompt template not found, name={}", name);
            throw new BusinessException(ErrorCode.PROMPT_NOT_FOUND);
        }
        return prompt;
    }
}
