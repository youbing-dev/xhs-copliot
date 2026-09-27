-- V1.0.1 内容域表结构
CREATE TABLE IF NOT EXISTS `contents` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL,
    `title`        VARCHAR(200) DEFAULT NULL,
    `body`         TEXT,
    `tags`         VARCHAR(500) DEFAULT NULL,
    `cover_images` JSON         DEFAULT NULL,
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'draft' COMMENT 'draft/published/archived',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_contents_user_status` (`user_id`, `status`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `content_versions` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `content_id`   BIGINT       NOT NULL,
    `version_num`  INT          NOT NULL,
    `title`        VARCHAR(200) DEFAULT NULL,
    `body`         TEXT,
    `tags`         VARCHAR(500) DEFAULT NULL,
    `cover_images` JSON         DEFAULT NULL,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_versions_content` (`content_id`, `version_num` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `generation_records` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL,
    `content_id`    BIGINT       DEFAULT NULL,
    `ai_prompt_id`  BIGINT       DEFAULT NULL,
    `topic`         VARCHAR(200) DEFAULT NULL,
    `gen_type`      VARCHAR(20)  NOT NULL COMMENT 'title/body/tag/cover/humanize',
    `ai_provider`   VARCHAR(30) DEFAULT NULL,
    `tokens_used`   INT          NOT NULL DEFAULT 0,
    `score`         INT          DEFAULT NULL,
    `duration_ms`   INT          DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_generation_user_date` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `usage_quotas` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT   NOT NULL,
    `quota_date`       DATE     NOT NULL,
    `notes_generated`  INT      NOT NULL DEFAULT 0,
    `covers_generated` INT     NOT NULL DEFAULT 0,
    `rewrites_used`    INT      NOT NULL DEFAULT 0,
    `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_usage_quota_date` (`user_id`, `quota_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `ai_prompts` (
    `id`        BIGINT      NOT NULL AUTO_INCREMENT,
    `name`      VARCHAR(50) NOT NULL,
    `type`      VARCHAR(20) NOT NULL COMMENT 'body/title/tag/cover/humanize',
    `template`  TEXT        NOT NULL,
    `variables` TEXT        DEFAULT NULL,
    `version`   INT         NOT NULL DEFAULT 1,
    `enabled`   TINYINT(1)  NOT NULL DEFAULT 1,
    `updated_at` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_prompts_name_version` (`name`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
