package com.moxi.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户资料请求
 */
@Data
@NoArgsConstructor
public class UpdateProfileRequest {

    /**
     * 昵称
     */
    @Size(max = 30, message = "昵称长度不能超过30个字符")
    private String nickname;

    /**
     * 个人简介
     */
    @Size(max = 200, message = "简介长度不能超过200个字符")
    private String bio;

    /**
     * 博主类型
     */
    @Size(max = 50, message = "博主类型长度不能超过50个字符")
    private String bloggerType;

    /**
     * 头像URL
     */
    @Size(max = 500, message = "头像URL长度不能超过500个字符")
    private String avatarUrl;
}
