package com.moxi.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moxi.common.exception.BusinessException;
import com.moxi.common.response.ErrorCode;
import com.moxi.user.domain.UserProfile;
import com.moxi.user.domain.UserSettings;
import com.moxi.user.dto.ProfileResponse;
import com.moxi.user.dto.SettingsResponse;
import com.moxi.user.dto.UpdateProfileRequest;
import com.moxi.user.dto.UpdateSettingsRequest;
import com.moxi.user.repository.UserProfileMapper;
import com.moxi.user.repository.UserSettingsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileMapper userProfileMapper;
    private final UserSettingsMapper userSettingsMapper;

    @Override
    public ProfileResponse getProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
        if (profile == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return new ProfileResponse(
                profile.getId(),
                profile.getNickname(),
                profile.getAvatarUrl(),
                profile.getBio(),
                profile.getBloggerType()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );
        if (profile == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 仅更新非空字段
        if (req.getNickname() != null) {
            profile.setNickname(req.getNickname());
        }
        if (req.getBio() != null) {
            profile.setBio(req.getBio());
        }
        if (req.getBloggerType() != null) {
            profile.setBloggerType(req.getBloggerType());
        }
        if (req.getAvatarUrl() != null) {
            profile.setAvatarUrl(req.getAvatarUrl());
        }
        userProfileMapper.updateById(profile);
    }

    @Override
    public SettingsResponse getSettings(Long userId) {
        UserSettings settings = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId)
        );
        if (settings == null) {
            // 未找到设置记录时返回默认值
            return new SettingsResponse(true, true, true);
        }
        return new SettingsResponse(
                settings.getNotifyEmail(),
                settings.getNotifySms(),
                settings.getNotifyPromotions()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSettings(Long userId, UpdateSettingsRequest req) {
        UserSettings settings = userSettingsMapper.selectOne(
                new LambdaQueryWrapper<UserSettings>().eq(UserSettings::getUserId, userId)
        );
        if (settings == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // 仅更新非空字段
        if (req.getNotifyEmail() != null) {
            settings.setNotifyEmail(req.getNotifyEmail());
        }
        if (req.getNotifySms() != null) {
            settings.setNotifySms(req.getNotifySms());
        }
        if (req.getNotifyPromotions() != null) {
            settings.setNotifyPromotions(req.getNotifyPromotions());
        }
        userSettingsMapper.updateById(settings);
    }
}
