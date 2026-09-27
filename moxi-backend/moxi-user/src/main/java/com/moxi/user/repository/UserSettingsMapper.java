package com.moxi.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxi.user.domain.UserSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户设置 Mapper
 */
@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {
}
