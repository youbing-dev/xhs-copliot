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
 * AI 生成记录实体，对应 generation_records 表。
 *
 * <p>每次调用 AI 网关生成内容后，都会落库一条记录，
 * 用于计费、配额统计与效果分析。</p>
 */
@Data
@NoArgsConstructor
@TableName("generation_records")
public class GenerationRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 关联内容 ID，可为空（直接生成时尚未落库内容）
     */
    private Long contentId;

    /**
     * 使用的 AI Prompt 模板 ID，可为空
     */
    private Long aiPromptId;

    /**
     * 生成主题/选题
     */
    private String topic;

    /**
     * 生成类型: title/body/tag/cover/humanize
     */
    private String genType;

    /**
     * 实际使用的 AI 供应商标识
     */
    private String aiProvider;

    /**
     * 本次生成消耗的 token 数
     */
    private Integer tokensUsed;

    /**
     * 生成内容质量评分，可为空
     */
    private Integer score;

    /**
     * 生成耗时（毫秒）
     */
    private Integer durationMs;

    /**
     * 创建时间，由 MetaObjectHandler 在插入时填充
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
