package com.moxi.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.user.domain.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户资料 Mapper
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
