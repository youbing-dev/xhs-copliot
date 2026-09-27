package com.moxi.billing.service;

import com.moxi.billing.dto.CreateOrderRequest;
import com.moxi.billing.dto.OrderListResponse;
import com.moxi.billing.dto.OrderResponse;
import com.moxi.billing.dto.PlanResponse;

import java.util.List;

/**
 * 计费服务接口
 */
public interface BillingService {

    /**
     * 获取所有会员套餐
     *
     * @return 套餐列表
     */
    List<PlanResponse> getAllPlans();

    /**
     * 创建订单
     *
     * @param userId 用户ID
     * @param req    创建订单请求
     * @return 订单响应
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest req);

    /**
     * 分页获取用户订单列表
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页数量
     * @return 订单列表响应
     */
    OrderListResponse getOrders(Long userId, int page, int size);

    /**
     * 获取订单详情
     *
     * @param userId  用户ID
     * @param orderId 订单ID
     * @return 订单响应
     */
    OrderResponse getOrderDetail(Long userId, Long orderId);

    /**
     * 处理支付回调
     *
     * @param transactionId  第三方交易号
     * @param orderId        订单ID
     * @param paymentMethod  支付方式
     */
    void handlePaymentCallback(String transactionId, Long orderId, String paymentMethod);
}
