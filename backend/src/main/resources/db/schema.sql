-- ============================================================
-- RAG System 完整建表脚本（全新环境使用）
-- 已包含 P0/P1 加固后的 role / owner_id / is_public 字段
--
-- 用法：
--   mysql -u root -p < src/main/resources/db/schema.sql
--
-- 注意：如果你是从旧版本升级，请改执行 db/migration/V1__p0_p1_hardening.sql，
--       不要重复执行本脚本中的 ALTER 语句。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `RAG_system`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE `RAG_system`;

-- 用户
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '业务用户ID（JWT 使用）',
    `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码哈希（BCrypt）',
    `salt`        VARCHAR(64)  DEFAULT NULL COMMENT '旧版 SHA-256 盐值，保留兼容',
    `email`       VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(512) DEFAULT NULL COMMENT '头像地址',
    `age`         INT          DEFAULT NULL COMMENT '年龄',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'ADMIN' COMMENT 'USER / ADMIN',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

-- 用户配额与模型配置
CREATE TABLE IF NOT EXISTS `user_config` (
    `user_id`             BIGINT      NOT NULL COMMENT '关联 user.user_id',
    `api_key`             VARCHAR(255) DEFAULT NULL COMMENT '用户 API Key（AES-GCM 加密）',
    `preferred_model`     VARCHAR(64)  DEFAULT NULL COMMENT '默认模型',
    `total_token_quota`   BIGINT      NOT NULL DEFAULT 0 COMMENT '总 Token 额度',
    `used_token_quota`    BIGINT      NOT NULL DEFAULT 0 COMMENT '已使用 Token',
    `max_knowledge_bases` INT         NOT NULL DEFAULT 5 COMMENT '最大知识库数量',
    `max_storage_size`    BIGINT      NOT NULL DEFAULT 1024 COMMENT '最大存储（MB）',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_user_config_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户配置表';

-- 知识库文件
CREATE TABLE IF NOT EXISTS `knowledge_file` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `file_name`   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_type`   VARCHAR(16)  NOT NULL COMMENT 'pdf / xlsx / docx',
    `file_url`    VARCHAR(512) NOT NULL COMMENT 'OSS 存储路径',
    `file_size`   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    `owner_id`    BIGINT       DEFAULT NULL COMMENT '上传者 user.user_id',
    `is_public`   TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否公开给其他用户检索',
    `upload_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_file_type` (`file_type`),
    KEY `idx_owner_id` (`owner_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文件表';

-- 敏感词
CREATE TABLE IF NOT EXISTS `sensitive_word` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `word`        VARCHAR(128) NOT NULL COMMENT '敏感词',
    `category`    VARCHAR(32)  DEFAULT NULL COMMENT '分类',
    `level`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=替换 2=拦截',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`),
    KEY `idx_category_level` (`category`, `level`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '敏感词表';

-- AI 对话日志
CREATE TABLE IF NOT EXISTS `ai_chat_log` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT      NOT NULL COMMENT '关联 user.user_id',
    `question`       TEXT        NOT NULL COMMENT '用户问题',
    `answer`         TEXT        NOT NULL COMMENT 'AI 回答',
    `model_name`     VARCHAR(64) DEFAULT NULL COMMENT '模型名称',
    `tokens_used`    BIGINT      NOT NULL DEFAULT 0 COMMENT '消耗 Token',
    `source_context` TEXT                 DEFAULT NULL COMMENT '引用来源',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    CONSTRAINT `fk_chat_log_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 对话日志表';

-- 文档分块（预留表，当前向量主要存 Redis）
CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `file_id`     BIGINT NOT NULL COMMENT '关联 knowledge_file.id',
    `chunk_text`  TEXT   NOT NULL COMMENT '分块文本',
    `chunk_index` INT    NOT NULL DEFAULT 0 COMMENT '分块序号',
    `metadata`    JSON            DEFAULT NULL COMMENT '来源文件、页码等',
    PRIMARY KEY (`id`),
    KEY `idx_file_id` (`file_id`),
    KEY `idx_chunk_index` (`file_id`, `chunk_index`),
    CONSTRAINT `fk_chunk_file` FOREIGN KEY (`file_id`) REFERENCES `knowledge_file` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT = '文档分块表';

-- 注册账号后，把第一个账号设为管理员（把 username 改成你的账号）：
-- UPDATE `user` SET `role` = 'ADMIN' WHERE `username` = 'your_username';
