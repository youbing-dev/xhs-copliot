package com.moxi.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送验证码请求
 */
@Data
@NoArgsConstructor
public class SendCodeRequest {

    /**
     * 发送类型: phone / email
     */
    @NotBlank(message = "发送类型不能为空")
    @Pattern(regexp = "^(phone|email)$", message = "发送类型必须为 phone 或 email")
    private String type;

    /**
     * 发送目标（手机号或邮箱）
     */
    @NotBlank(message = "发送目标不能为空")
    private String target;
}
