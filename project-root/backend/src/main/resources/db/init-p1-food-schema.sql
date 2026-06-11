-- P1 Food module database initialization script.
-- Scope: table structure only. No sample data or admin import logic is included.

USE `tour_system`;

CREATE TABLE IF NOT EXISTS `food` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `destination_id` BIGINT NOT NULL COMMENT '所属目的地 ID',
  `facility_id` BIGINT NULL COMMENT '所属设施 ID，可为空',
  `name` VARCHAR(100) NOT NULL COMMENT '美食名称',
  `food_type` VARCHAR(50) NULL COMMENT '菜系',
  `shop_name` VARCHAR(100) NULL COMMENT '店铺或窗口名称',
  `description` TEXT NULL COMMENT '美食描述',
  `heat_score` DECIMAL(5,2) NULL DEFAULT 0 COMMENT '热度分',
  `rating_score` DECIMAL(3,2) NULL DEFAULT 0 COMMENT '评分',
  `avg_price` DECIMAL(8,2) NULL COMMENT '平均价格',
  `cover_url` VARCHAR(255) NULL COMMENT '封面图地址',
  `lng` DECIMAL(10,6) NULL COMMENT '经度',
  `lat` DECIMAL(10,6) NULL COMMENT '纬度',
  PRIMARY KEY (`id`),
  KEY `idx_food_destination_id` (`destination_id`),
  KEY `idx_food_facility_id` (`facility_id`),
  KEY `idx_food_name` (`name`),
  KEY `idx_food_type` (`food_type`),
  KEY `idx_food_shop_name` (`shop_name`),
  CONSTRAINT `fk_food_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_food_facility`
    FOREIGN KEY (`facility_id`) REFERENCES `facility` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='美食表';

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
