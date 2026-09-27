package com.moxi.billing.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.billing.domain.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
