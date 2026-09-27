package com.moxi.billing.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单响应
 */
@Data
public class OrderResponse {

    private Long id;
    private Long planId;
    private String planName;
    private Integer amount;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
