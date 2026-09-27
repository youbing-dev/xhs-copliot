package com.moxi.billing.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.billing.domain.MembershipPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员套餐 Mapper
 */
@Mapper
public interface MembershipPlanMapper extends BaseMapper<MembershipPlan> {
}
