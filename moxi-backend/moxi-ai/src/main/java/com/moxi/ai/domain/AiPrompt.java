package com.moxi.ai.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI Prompt 模板实体，对应 ai_prompts 表。
 *
 * <p>同一名称的模板支持多版本，启用版本通过 enabled 字段控制，
 * 取用时按版本号倒序选取最新启用的记录。</p>
 */
@Data
@NoArgsConstructor
@TableName("ai_prompts")
public class AiPrompt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板名称（与版本组合唯一）
     */
    private String name;

    /**
     * 模板类型: body/title/tag/cover/humanize
     */
    private String type;

    /**
     * 模板正文，含 {{变量名}} 占位符
     */
    private String template;

    /**
     * 变量声明，可用于校验或文档展示，可为空
     */
    private String variables;

    /**
     * 模板版本号
     */
    private Integer version;

    /**
     * 是否启用: true-启用, false-禁用
     */
    private Boolean enabled;

    /**
     * 更新时间，由 MetaObjectHandler 在写入时填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
