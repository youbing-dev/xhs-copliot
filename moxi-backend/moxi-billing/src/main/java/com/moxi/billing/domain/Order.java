package com.moxi.billing.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("orders")
public class Order {

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
     * 订单金额（分）
     */
    private Integer amount;

    /**
     * 订单状态: PENDING/PAID/REFUNDED/CANCELLED
     */
    private String status;

    /**
     * 支付方式: wechat/alipay
     */
    private String paymentMethod;

    /**
     * 第三方交易号
     */
    private String transactionId;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
