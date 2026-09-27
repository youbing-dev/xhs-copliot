package com.moxi.user.controller;

import com.moxi.common.response.ApiResult;
import com.moxi.user.dto.ProfileResponse;
import com.moxi.user.dto.SettingsResponse;
import com.moxi.user.dto.UpdateProfileRequest;
import com.moxi.user.dto.UpdateSettingsRequest;
import com.moxi.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取用户资料（需要认证）
     */
    @GetMapping("/profile")
    public ApiResult<ProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(userService.getProfile(userId));
    }

    /**
     * 更新用户资料（需要认证）
     */
    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest req,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateProfile(userId, req);
        return ApiResult.success();
    }

    /**
     * 获取用户设置（需要认证）
     */
    @GetMapping("/settings")
    public ApiResult<SettingsResponse> getSettings(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(userService.getSettings(userId));
    }

    /**
     * 更新用户设置（需要认证）
     */
    @PutMapping("/settings")
    public ApiResult<Void> updateSettings(@Valid @RequestBody UpdateSettingsRequest req,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateSettings(userId, req);
        return ApiResult.success();
    }
}
