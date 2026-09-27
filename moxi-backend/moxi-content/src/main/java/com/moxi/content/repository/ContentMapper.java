package com.moxi.content.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.content.domain.Content;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容表 Mapper，基于 MyBatis-Plus BaseMapper 提供基础 CRUD。
 */
@Mapper
public interface ContentMapper extends BaseMapper<Content> {
}
