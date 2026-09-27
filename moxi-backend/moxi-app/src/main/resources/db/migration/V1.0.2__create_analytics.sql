-- V1.0.2 数据分析域表结构
CREATE TABLE IF NOT EXISTS `hot_topics` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `title`             VARCHAR(200) NOT NULL,
    `note_count`        INT          NOT NULL DEFAULT 0,
    `interaction_count` INT          NOT NULL DEFAULT 0,
    `trend`             VARCHAR(10)  NOT NULL DEFAULT 'flat' COMMENT 'up/down/flat',
    `period`            VARCHAR(20)  NOT NULL DEFAULT '7d',
    `heat_score`        INT          NOT NULL DEFAULT 0,
    `collected_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hot_topics_period_heat` (`period`, `heat_score` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tag_analytics` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `hot_topic_id`      BIGINT       DEFAULT NULL,
    `tag_name`          VARCHAR(50)  NOT NULL,
    `heat_score`        INT          NOT NULL DEFAULT 0,
    `note_count`        INT          NOT NULL DEFAULT 0,
    `interaction_avg`   DECIMAL(10,2) NOT NULL DEFAULT 0,
    `period`            VARCHAR(20)  NOT NULL DEFAULT '7d',
    `collected_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tag_analytics_period` (`period`, `heat_score` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `competitor_profiles` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(50)  NOT NULL,
    `avatar_url`       VARCHAR(255) DEFAULT NULL,
    `followers`         INT          NOT NULL DEFAULT 0,
    `avg_interaction`  INT          NOT NULL DEFAULT 0,
    `trend`            VARCHAR(10)  NOT NULL DEFAULT 'flat',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `competitor_topics` (
    `id`             BIGINT   NOT NULL AUTO_INCREMENT,
    `competitor_id`  BIGINT   NOT NULL,
    `hot_topic_id`   BIGINT   NOT NULL,
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_comp_topics_competitor` (`competitor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
