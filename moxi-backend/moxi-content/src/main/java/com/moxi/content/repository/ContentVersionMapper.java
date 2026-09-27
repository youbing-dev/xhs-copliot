package com.moxi.content.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.content.domain.ContentVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容版本表 Mapper，基于 MyBatis-Plus BaseMapper 提供基础 CRUD。
 */
@Mapper
public interface ContentVersionMapper extends BaseMapper<ContentVersion> {
}
