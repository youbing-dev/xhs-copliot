package com.moxi.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户资料响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {

    /**
     * 资料ID
     */
    private Long id;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 博主类型
     */
    private String bloggerType;
}
