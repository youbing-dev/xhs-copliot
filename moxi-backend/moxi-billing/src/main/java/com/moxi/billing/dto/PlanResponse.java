package com.moxi.billing.dto;

import lombok.Data;

import java.util.List;

/**
 * 套餐响应
 */
@Data
public class PlanResponse {

    private Long id;
    private String name;
    private Integer priceMonthly;
    private Integer priceAnnual;
    private Integer dailyNoteQuota;
    private Integer monthlyCoverQuota;
    private Boolean unlimitedNotes;
    private Integer rewriteQuota;
    private List<String> features;
}
