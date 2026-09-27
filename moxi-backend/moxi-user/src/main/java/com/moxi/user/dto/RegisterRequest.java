package com.moxi.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注册请求
 */
@Data
@NoArgsConstructor
public class RegisterRequest {

    /**
     * 注册类型: phone / email
     */
    @NotBlank(message = "注册类型不能为空")
    @Pattern(regexp = "^(phone|email)$", message = "注册类型必须为 phone 或 email")
    private String type;

    /**
     * 手机号（type 为 phone 时必填）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱（type 为 email 时必填）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码（6位数字）
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String code;

    /**
     * 密码（6-20位）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    /**
     * 昵称（可选）
     */
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;
}
