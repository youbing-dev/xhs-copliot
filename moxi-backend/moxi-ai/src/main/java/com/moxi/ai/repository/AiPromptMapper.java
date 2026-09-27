package com.moxi.ai.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.ai.domain.AiPrompt;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI Prompt 模板表 Mapper，基于 MyBatis-Plus BaseMapper 提供基础 CRUD。
 */
@Mapper
public interface AiPromptMapper extends BaseMapper<AiPrompt> {
}
