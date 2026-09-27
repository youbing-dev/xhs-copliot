-- V1.0.0 用户域表结构
CREATE TABLE IF NOT EXISTS `users` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `phone`         VARCHAR(11)  DEFAULT NULL,
    `email`         VARCHAR(128) DEFAULT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '0-未激活 1-正常 2-封禁',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_phone` (`phone`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_profiles` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `nickname`     VARCHAR(50)  DEFAULT NULL,
    `avatar_url`   VARCHAR(255) DEFAULT NULL,
    `bio`          VARCHAR(200) DEFAULT NULL,
    `blogger_type` VARCHAR(50)  DEFAULT NULL COMMENT '自我提升/职场/读书/副业等',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_profiles_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_settings` (
    `id`                 BIGINT  NOT NULL AUTO_INCREMENT,
    `user_id`            BIGINT  NOT NULL,
    `notify_email`       TINYINT(1) NOT NULL DEFAULT 1,
    `notify_sms`         TINYINT(1) NOT NULL DEFAULT 0,
    `notify_promotions`  TINYINT(1) NOT NULL DEFAULT 1,
    `updated_at`         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_settings_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `membership_plans` (
    `id`                 BIGINT      NOT NULL AUTO_INCREMENT,
    `name`               VARCHAR(20) NOT NULL,
    `price_monthly`      INT         NOT NULL COMMENT '分',
    `price_annual`       INT         NOT NULL COMMENT '分',
    `daily_note_quota`   INT         NOT NULL DEFAULT 3,
    `monthly_cover_quota` INT        NOT NULL DEFAULT 5,
    `unlimited_notes`    TINYINT(1)  NOT NULL DEFAULT 0,
    `rewrite_quota`      INT         NOT NULL DEFAULT 0,
    `features`           JSON        DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `memberships` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL,
    `plan_id`     BIGINT      NOT NULL,
    `start_date`  DATE        NOT NULL,
    `end_date`    DATE        NOT NULL,
    `status`      VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active/expired/cancelled',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `active_user_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `status` = 'active' THEN `user_id` ELSE NULL END) STORED,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_membership_active` (`active_user_id`),
    KEY `idx_memberships_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 套餐种子数据
INSERT INTO `membership_plans` (`name`, `price_monthly`, `price_annual`, `daily_note_quota`, `monthly_cover_quota`, `unlimited_notes`, `rewrite_quota`, `features`)
VALUES
('free', 0, 0, 3, 0, 0, 0, '["basic_generate","limited_quota"]'),
('basic', 2900, 29000, 30, 10, 0, 10, '["basic_generate","cover_generate","humanize_rewrite"]'),
('pro', 5900, 59000, 0, 45, 1, 50, '["unlimited_notes","cover_generate","humanize_rewrite","priority_support"]');
