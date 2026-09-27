package com.moxi.billing.controller;

import com.moxi.billing.dto.CreateOrderRequest;
import com.moxi.billing.dto.OrderListResponse;
import com.moxi.billing.dto.OrderResponse;
import com.moxi.billing.dto.PlanResponse;
import com.moxi.billing.service.BillingService;
import com.moxi.common.response.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 计费控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /**
     * 获取所有会员套餐（无需认证）
     */
    @GetMapping("/plans")
    public ApiResult<List<PlanResponse>> getPlans() {
        return ApiResult.success(billingService.getAllPlans());
    }

    /**
     * 创建订单（需要认证）
     */
    @PostMapping("/orders")
    public ApiResult<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest req,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(billingService.createOrder(userId, req));
    }

    /**
     * 获取用户订单列表（需要认证）
     */
    @GetMapping("/orders")
    public ApiResult<OrderListResponse> getOrders(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(billingService.getOrders(userId, page, size));
    }

    /**
     * 获取订单详情（需要认证）
     */
    @GetMapping("/orders/{id}")
    public ApiResult<OrderResponse> getOrderDetail(@PathVariable Long id,
                                                   HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(billingService.getOrderDetail(userId, id));
    }

    /**
     * 微信支付回调（无需认证，占位实现）
     */
    @PostMapping("/callback/wechat")
    public ApiResult<Void> wechatCallback() {
        return ApiResult.success();
    }

    /**
     * 支付宝支付回调（无需认证，占位实现）
     */
    @PostMapping("/callback/alipay")
    public ApiResult<Void> alipayCallback() {
        return ApiResult.success();
    }
}
