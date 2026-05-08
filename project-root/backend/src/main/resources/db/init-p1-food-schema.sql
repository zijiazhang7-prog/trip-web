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
