package com.moxi.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户设置响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsResponse {

    /**
     * 邮件通知开关
     */
    private Boolean notifyEmail;

    /**
     * 短信通知开关
     */
    private Boolean notifySms;

    /**
     * 促销通知开关
     */
    private Boolean notifyPromotions;
}
