-- Demo data schema migration.
-- Scope: add presentation fields and comment tables for an existing tour_system database.

USE `tour_system`;

DELIMITER //

DROP PROCEDURE IF EXISTS add_column_if_missing//

CREATE PROCEDURE add_column_if_missing(
  IN target_table_name VARCHAR(64),
  IN target_column_name VARCHAR(64),
  IN column_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = target_table_name
      AND column_name = target_column_name
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE `', target_table_name, '` ADD COLUMN `', target_column_name, '` ', column_definition);
    PREPARE statement FROM @ddl;
    EXECUTE statement;
    DEALLOCATE PREPARE statement;
  END IF;
END//

DELIMITER ;

CALL add_column_if_missing('facility', 'address', 'VARCHAR(255) NULL COMMENT ''设施地址或位置描述'' AFTER `description`');
CALL add_column_if_missing('facility', 'tel', 'VARCHAR(50) NULL COMMENT ''联系电话'' AFTER `address`');
CALL add_column_if_missing('facility', 'cover_url', 'VARCHAR(255) NULL COMMENT ''封面图地址'' AFTER `tel`');
CALL add_column_if_missing('food', 'lng', 'DECIMAL(10,6) NULL COMMENT ''经度'' AFTER `cover_url`');
CALL add_column_if_missing('food', 'lat', 'DECIMAL(10,6) NULL COMMENT ''纬度'' AFTER `lng`');

DROP PROCEDURE add_column_if_missing;

CREATE TABLE IF NOT EXISTS `destination_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `destination_id` BIGINT NOT NULL COMMENT '所属目的地 ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户 ID',
  `parent_comment_id` BIGINT NULL COMMENT '父评论 ID，顶级评论为空',
  `content_text` TEXT NOT NULL COMMENT '评论正文',
  `media_url` VARCHAR(255) NULL COMMENT '评论附图地址',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1 正常，0 隐藏，2 删除/违规',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_destination_comment_destination_id` (`destination_id`),
  KEY `idx_destination_comment_user_id` (`user_id`),
  KEY `idx_destination_comment_parent_id` (`parent_comment_id`),
  KEY `idx_destination_comment_status_created` (`destination_id`, `status`, `created_at`),
  CONSTRAINT `fk_destination_comment_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_destination_comment_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_destination_comment_parent`
    FOREIGN KEY (`parent_comment_id`) REFERENCES `destination_comment` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目的地评论表';

CREATE TABLE IF NOT EXISTS `food_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `food_id` BIGINT NOT NULL COMMENT '所属美食 ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户 ID',
  `parent_comment_id` BIGINT NULL COMMENT '父评论 ID，顶级评论为空',
  `content_text` TEXT NOT NULL COMMENT '评论正文',
  `media_url` VARCHAR(255) NULL COMMENT '评论附图地址',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1 正常，0 隐藏，2 删除/违规',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_food_comment_food_id` (`food_id`),
  KEY `idx_food_comment_user_id` (`user_id`),
  KEY `idx_food_comment_parent_id` (`parent_comment_id`),
  KEY `idx_food_comment_status_created` (`food_id`, `status`, `created_at`),
  CONSTRAINT `fk_food_comment_food`
    FOREIGN KEY (`food_id`) REFERENCES `food` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_food_comment_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_food_comment_parent`
    FOREIGN KEY (`parent_comment_id`) REFERENCES `food_comment` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='美食评论表';

CREATE TABLE IF NOT EXISTS `diary_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `diary_id` BIGINT NOT NULL COMMENT '所属日记 ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户 ID',
  `parent_comment_id` BIGINT NULL COMMENT '父评论 ID，顶级评论为空',
  `content_text` TEXT NOT NULL COMMENT '评论正文',
  `media_url` VARCHAR(255) NULL COMMENT '评论附图地址',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reply_count` INT NOT NULL DEFAULT 0 COMMENT '回复数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1 正常，0 隐藏，2 删除/违规',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_diary_comment_diary_id` (`diary_id`),
  KEY `idx_diary_comment_user_id` (`user_id`),
  KEY `idx_diary_comment_parent_id` (`parent_comment_id`),
  KEY `idx_diary_comment_status_created` (`diary_id`, `status`, `created_at`),
  CONSTRAINT `fk_diary_comment_diary`
    FOREIGN KEY (`diary_id`) REFERENCES `diary` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_diary_comment_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_diary_comment_parent`
    FOREIGN KEY (`parent_comment_id`) REFERENCES `diary_comment` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日记评论表';
