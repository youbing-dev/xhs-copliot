package com.moxi.user.service;

import com.moxi.user.dto.LoginRequest;
import com.moxi.user.dto.LoginResponse;
import com.moxi.user.dto.RegisterRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 发送验证码
     *
     * @param type   类型（phone / email）
     * @param target 目标（手机号或邮箱）
     * @param ip     请求方IP
     */
    void sendCode(String type, String target, String ip);

    /**
     * 注册
     *
     * @param req 注册请求
     * @return 登录响应
     */
    LoginResponse register(RegisterRequest req);

    /**
     * 登录
     *
     * @param req 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest req);

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 登录响应
     */
    LoginResponse refresh(String refreshToken);

    /**
     * 登出
     *
     * @param accessToken 访问令牌
     * @param userId      用户ID
     */
    void logout(String accessToken, Long userId);
}
