-- ============================================================
-- P0/P1 安全加固迁移脚本
-- 说明：项目未接入 Flyway，需要手动执行一次（mysql -u root -p RAG_system < 本文件）
-- ============================================================

-- 1. 用户角色（USER / ADMIN）
ALTER TABLE `user`
    ADD COLUMN `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN' AFTER `age`;

-- 将你的主账号设为管理员（按需修改用户名）
SET @admin_username = 'ccc123';
UPDATE `user`
SET `role` = 'ADMIN'
WHERE `username` = @admin_username;

-- 2. 知识库文件归属与公开标记
ALTER TABLE `knowledge_file`
    ADD COLUMN `owner_id` BIGINT NULL COMMENT '上传者 user.user_id' AFTER `file_size`;

ALTER TABLE `knowledge_file`
    ADD COLUMN `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开' AFTER `owner_id`;

ALTER TABLE `knowledge_file`
    ADD INDEX `idx_owner_id` (`owner_id`);

-- 3. 历史文件归属到管理员账号，避免升级后列表为空
UPDATE `knowledge_file`
SET `owner_id` = (SELECT `user_id` FROM `user` WHERE `username` = @admin_username)
WHERE `owner_id` IS NULL;
