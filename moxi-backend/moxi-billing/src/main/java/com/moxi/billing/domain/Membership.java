package com.moxi.billing.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员实体
 * <p>
 * 注意：active_user_id 为数据库生成列，此处不映射。
 */
@Data
@TableName("memberships")
public class Membership {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 套餐ID
     */
    private Long planId;

    /**
     * 会员开始日期
     */
    private LocalDate startDate;

    /**
     * 会员结束日期
     */
    private LocalDate endDate;

    /**
     * 会员状态: active/expired/cancelled
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
