package com.moxi.user.controller;

import com.moxi.common.response.ApiResult;
import com.moxi.user.dto.LoginRequest;
import com.moxi.user.dto.LoginResponse;
import com.moxi.user.dto.ProfileResponse;
import com.moxi.user.dto.RegisterRequest;
import com.moxi.user.dto.SendCodeRequest;
import com.moxi.user.service.AuthService;
import com.moxi.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 发送验证码（无需认证）
     */
    @PostMapping("/send-code")
    public ApiResult<Void> sendCode(@Valid @RequestBody SendCodeRequest req,
                                   HttpServletRequest request) {
        String ip = getClientIp(request);
        authService.sendCode(req.getType(), req.getTarget(), ip);
        return ApiResult.success();
    }

    /**
     * 注册（无需认证）
     */
    @PostMapping("/register")
    public ApiResult<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResult.success(authService.register(req));
    }

    /**
     * 登录（无需认证）
     */
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResult.success(authService.login(req));
    }

    /**
     * 刷新令牌（无需认证，请求体中携带 refreshToken）
     */
    @PostMapping("/refresh")
    public ApiResult<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ApiResult.success(authService.refresh(refreshToken));
    }

    /**
     * 登出（需要认证）
     */
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request,
                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = (Long) request.getAttribute("userId");
        String token = extractToken(authHeader);
        authService.logout(token, userId);
        return ApiResult.success();
    }

    /**
     * 获取当前登录用户信息（需要认证）
     */
    @GetMapping("/me")
    public ApiResult<ProfileResponse> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(userService.getProfile(userId));
    }

    /**
     * 从请求头中提取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从 Authorization 头中提取 Bearer 令牌
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
