package com.moxi.billing.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxi.billing.domain.Membership;
import com.moxi.billing.domain.MembershipPlan;
import com.moxi.billing.domain.Order;
import com.moxi.billing.dto.CreateOrderRequest;
import com.moxi.billing.dto.OrderListResponse;
import com.moxi.billing.dto.OrderResponse;
import com.moxi.billing.dto.PlanResponse;
import com.moxi.billing.repository.MembershipMapper;
import com.moxi.billing.repository.MembershipPlanMapper;
import com.moxi.billing.repository.OrderMapper;
import com.moxi.common.constant.RedisKeys;
import com.moxi.common.exception.BusinessException;
import com.moxi.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 计费服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final MembershipPlanMapper membershipPlanMapper;
    private final OrderMapper orderMapper;
    private final MembershipMapper membershipMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<PlanResponse> getAllPlans() {
        List<MembershipPlan> plans = membershipPlanMapper.selectList(
                new LambdaQueryWrapper<MembershipPlan>().orderByAsc(MembershipPlan::getId)
        );
        return plans.stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
        MembershipPlan plan = membershipPlanMapper.selectById(req.getPlanId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }

        int amount;
        if ("annual".equals(req.getBillingCycle())) {
            amount = plan.getPriceAnnual();
        } else {
            amount = plan.getPriceMonthly();
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setPlanId(req.getPlanId());
        order.setAmount(amount);
        order.setStatus("PENDING");
        order.setPaymentMethod(req.getPaymentMethod());
        orderMapper.insert(order);

        return toOrderResponse(order, plan.getName());
    }

    @Override
    public OrderListResponse getOrders(Long userId, int page, int size) {
        Page<Order> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);
        Page<Order> result = orderMapper.selectPage(pageParam, wrapper);

        // 批量查询套餐名称
        List<Long> planIds = result.getRecords().stream()
                .map(Order::getPlanId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> planNameMap = new HashMap<>();
        if (!planIds.isEmpty()) {
            List<MembershipPlan> plans = membershipPlanMapper.selectBatchIds(planIds);
            for (MembershipPlan plan : plans) {
                planNameMap.put(plan.getId(), plan.getName());
            }
        }

        List<OrderResponse> items = result.getRecords().stream()
                .map(o -> toOrderResponse(o, planNameMap.get(o.getPlanId())))
                .collect(Collectors.toList());

        OrderListResponse resp = new OrderListResponse();
        resp.setTotal(result.getTotal());
        resp.setPage(page);
        resp.setSize(size);
        resp.setItems(items);
        return resp;
    }

    @Override
    public OrderResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        MembershipPlan plan = membershipPlanMapper.selectById(order.getPlanId());
        String planName = plan != null ? plan.getName() : null;
        return toOrderResponse(order, planName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(String transactionId, Long orderId, String paymentMethod) {
        // 1. Redis SETNX 幂等去重
        String key = RedisKeys.payCallbackDedup(transactionId);
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(acquired)) {
            log.info("支付回调已处理，跳过，transactionId={}", transactionId);
            return;
        }

        // 2. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("支付回调订单不存在，orderId={}", orderId);
            return;
        }
        if ("PAID".equals(order.getStatus())) {
            log.info("订单已支付，跳过，orderId={}", orderId);
            return;
        }

        // 3. 更新订单状态
        order.setStatus("PAID");
        order.setPaymentMethod(paymentMethod);
        order.setTransactionId(transactionId);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        // 4. 根据套餐价格判断计费周期
        MembershipPlan plan = membershipPlanMapper.selectById(order.getPlanId());
        boolean annual = plan != null
                && plan.getPriceAnnual() != null
                && plan.getPriceAnnual().equals(order.getAmount());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = annual ? startDate.plusDays(365) : startDate.plusDays(30);

        // 5. 过期已有 active 会员
        Membership existing = membershipMapper.selectOne(
                new LambdaQueryWrapper<Membership>()
                        .eq(Membership::getUserId, order.getUserId())
                        .eq(Membership::getStatus, "active")
        );
        if (existing != null) {
            existing.setStatus("expired");
            membershipMapper.updateById(existing);
        }

        // 6. 创建新会员
        Membership membership = new Membership();
        membership.setUserId(order.getUserId());
        membership.setPlanId(order.getPlanId());
        membership.setStartDate(startDate);
        membership.setEndDate(endDate);
        membership.setStatus("active");
        membershipMapper.insert(membership);

        log.info("支付成功，userId={}, orderId={}, planId={}, cycle={}",
                order.getUserId(), orderId, order.getPlanId(), annual ? "annual" : "monthly");
    }

    /**
     * 套餐实体转响应 DTO（解析 features JSON）
     */
    private PlanResponse toPlanResponse(MembershipPlan plan) {
        PlanResponse resp = new PlanResponse();
        resp.setId(plan.getId());
        resp.setName(plan.getName());
        resp.setPriceMonthly(plan.getPriceMonthly());
        resp.setPriceAnnual(plan.getPriceAnnual());
        resp.setDailyNoteQuota(plan.getDailyNoteQuota());
        resp.setMonthlyCoverQuota(plan.getMonthlyCoverQuota());
        resp.setUnlimitedNotes(plan.getUnlimitedNotes());
        resp.setRewriteQuota(plan.getRewriteQuota());
        String featuresStr = plan.getFeatures();
        List<String> features = (featuresStr == null || featuresStr.isBlank())
                ? Collections.emptyList()
                : JSONUtil.toList(featuresStr, String.class);
        resp.setFeatures(features);
        return resp;
    }

    /**
     * 订单实体转响应 DTO
     */
    private OrderResponse toOrderResponse(Order order, String planName) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setPlanId(order.getPlanId());
        resp.setPlanName(planName);
        resp.setAmount(order.getAmount());
        resp.setStatus(order.getStatus());
        resp.setPaymentMethod(order.getPaymentMethod());
        resp.setTransactionId(order.getTransactionId());
        resp.setPaidAt(order.getPaidAt());
        resp.setCreatedAt(order.getCreatedAt());
        return resp;
    }
}
