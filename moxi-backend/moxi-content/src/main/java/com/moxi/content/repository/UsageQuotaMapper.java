package com.moxi.content.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.content.domain.UsageQuota;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UsageQuotaMapper extends BaseMapper<UsageQuota> {
}
