package com.moxi.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求
 */
@Data
@NoArgsConstructor
public class LoginRequest {

    /**
     * 账号（手机号或邮箱）
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
