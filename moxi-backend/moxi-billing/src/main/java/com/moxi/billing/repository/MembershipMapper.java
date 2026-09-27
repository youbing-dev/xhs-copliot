package com.moxi.billing.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.billing.domain.Membership;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员 Mapper
 */
@Mapper
public interface MembershipMapper extends BaseMapper<Membership> {
}
