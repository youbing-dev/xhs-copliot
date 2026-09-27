-- V1.0.3 计费域表结构
CREATE TABLE IF NOT EXISTS `orders` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT      NOT NULL,
    `plan_id`        BIGINT      NOT NULL,
    `amount`         INT         NOT NULL COMMENT '分',
    `status`         VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/REFUNDED/CANCELLED',
    `payment_method` VARCHAR(20) DEFAULT NULL COMMENT 'wechat/alipay',
    `transaction_id` VARCHAR(64) DEFAULT NULL,
    `paid_at`        DATETIME    DEFAULT NULL,
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_orders_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
