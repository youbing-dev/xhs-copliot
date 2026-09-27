package com.moxi.ai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.ai.domain.GenerationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 生成记录表 Mapper，基于 MyBatis-Plus BaseMapper 提供基础 CRUD。
 */
@Mapper
public interface GenerationRecordMapper extends BaseMapper<GenerationRecord> {
}
