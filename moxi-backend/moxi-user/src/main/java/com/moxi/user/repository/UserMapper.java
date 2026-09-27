package com.moxi.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
