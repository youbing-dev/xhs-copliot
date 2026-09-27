package com.moxi.billing.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 会员套餐实体
 */
@Data
@TableName("membership_plans")
public class MembershipPlan {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 套餐名称
     */
    private String name;

    /**
     * 月付价格（分）
     */
    private Integer priceMonthly;

    /**
     * 年付价格（分）
     */
    private Integer priceAnnual;

    /**
     * 每日笔记额度
     */
    private Integer dailyNoteQuota;

    /**
     * 每月封面额度
     */
    private Integer monthlyCoverQuota;

    /**
     * 是否无限笔记
     */
    private Boolean unlimitedNotes;

    /**
     * 改写额度
     */
    private Integer rewriteQuota;

    /**
     * 套餐特性（JSON 字符串）
     */
    private String features;
}
