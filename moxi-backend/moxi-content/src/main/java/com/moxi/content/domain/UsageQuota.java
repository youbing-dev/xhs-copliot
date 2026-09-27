package com.moxi.content.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 使用额度实体，对应 usage_quotas 表。
 * 每日生成量由 Redis 实时计数，定时任务同步至此表持久化。
 */
@Data
@NoArgsConstructor
@TableName("usage_quotas")
public class UsageQuota {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate quotaDate;

    private Integer notesGenerated;

    private Integer coversGenerated;

    private Integer rewritesUsed;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
