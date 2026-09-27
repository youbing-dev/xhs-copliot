package com.moxi.user.service;

import com.moxi.user.dto.ProfileResponse;
import com.moxi.user.dto.SettingsResponse;
import com.moxi.user.dto.UpdateProfileRequest;
import com.moxi.user.dto.UpdateSettingsRequest;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取用户资料
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    ProfileResponse getProfile(Long userId);

    /**
     * 更新用户资料
     *
     * @param userId 用户ID
     * @param req    更新请求
     */
    void updateProfile(Long userId, UpdateProfileRequest req);

    /**
     * 获取用户设置
     *
     * @param userId 用户ID
     * @return 用户设置
     */
    SettingsResponse getSettings(Long userId);

    /**
     * 更新用户设置
     *
     * @param userId 用户ID
     * @param req    更新请求
     */
    void updateSettings(Long userId, UpdateSettingsRequest req);
}
