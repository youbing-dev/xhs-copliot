package com.moxi.billing.dto;

import lombok.Data;

import java.util.List;

/**
 * 订单列表响应
 */
@Data
public class OrderListResponse {

    private Long total;
    private Integer page;
    private Integer size;
    private List<OrderResponse> items;
}
