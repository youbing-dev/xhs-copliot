package com.moxi.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {

    /**
     * 套餐ID
     */
    @NotNull(message = "套餐ID不能为空")
    private Long planId;

    /**
     * 计费周期: monthly / annual
     */
    @NotBlank(message = "计费周期不能为空")
    @Pattern(regexp = "^(monthly|annual)$", message = "计费周期必须为 monthly 或 annual")
    private String billingCycle;

    /**
     * 支付方式: wechat / alipay
     */
    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "^(wechat|alipay)$", message = "支付方式必须为 wechat 或 alipay")
    private String paymentMethod;
}
