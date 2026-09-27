package com.moxi.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户设置请求
 */
@Data
@NoArgsConstructor
public class UpdateSettingsRequest {

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
