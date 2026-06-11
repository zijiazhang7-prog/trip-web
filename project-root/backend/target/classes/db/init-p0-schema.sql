-- P0 database initialization script for the personalized travel system.
-- Scope: table structure only. No sample data, auth logic, or file service logic is included.

CREATE DATABASE IF NOT EXISTS `tour_system`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `tour_system`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '加密后的密码',
  `nickname` VARCHAR(50) NULL COMMENT '用户昵称',
  `avatar_url` VARCHAR(255) NULL COMMENT '用户头像地址',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '用户角色，如 user/admin',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态，1 启用，0 禁用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `user_preference` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '关联用户 ID',
  `prefer_hot_level` INT NULL COMMENT '对热门程度的偏好',
  `prefer_theme` VARCHAR(255) NULL COMMENT '偏好主题，可存单值或多选序列',
  `prefer_food_type` VARCHAR(100) NULL COMMENT '偏好菜系',
  `prefer_crowd_level` INT NULL COMMENT '对拥挤度的接受程度',
  `travel_style` VARCHAR(50) NULL COMMENT '旅游风格，如小众/打卡/轻松',
  `custom_preference_text` TEXT NULL COMMENT '自由偏好描述文本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_preference_user_id` (`user_id`),
  CONSTRAINT `fk_user_preference_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户偏好表';

CREATE TABLE IF NOT EXISTS `destination` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '目的地名称',
  `type` VARCHAR(20) NOT NULL COMMENT '目的地类型，如 scenic/campus',
  `category` VARCHAR(50) NULL COMMENT '目的地分类',
  `city` VARCHAR(50) NULL COMMENT '所在城市',
  `description` TEXT NULL COMMENT '目的地简介',
  `heat_score` DECIMAL(5,2) NULL DEFAULT 0 COMMENT '热度分',
  `rating_score` DECIMAL(3,2) NULL DEFAULT 0 COMMENT '评分',
  `tag_json` TEXT NULL COMMENT '标签 JSON',
  `cover_url` VARCHAR(255) NULL COMMENT '封面图地址',
  `status` TINYINT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_destination_name` (`name`),
  KEY `idx_destination_type` (`type`),
  KEY `idx_destination_category` (`category`),
  KEY `idx_destination_heat_rating` (`heat_score`, `rating_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='目的地表';

CREATE TABLE IF NOT EXISTS `place` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `destination_id` BIGINT NOT NULL COMMENT '所属目的地 ID',
  `name` VARCHAR(100) NOT NULL COMMENT '场所名称',
  `place_type` VARCHAR(50) NOT NULL COMMENT '场所类型，如景点/教学楼/宿舍',
  `description` TEXT NULL COMMENT '场所简介',
  `lng` DECIMAL(10,6) NULL COMMENT '经度',
  `lat` DECIMAL(10,6) NULL COMMENT '纬度',
  `floor_info` VARCHAR(100) NULL COMMENT '楼层信息',
  `heat_score` DECIMAL(5,2) NULL DEFAULT 0 COMMENT '热度分',
  `rating_score` DECIMAL(3,2) NULL DEFAULT 0 COMMENT '评分',
  `open_time_rule` VARCHAR(255) NULL COMMENT '开放时间规则描述',
  `suggested_duration_min` INT NULL COMMENT '建议游玩时长（分钟）',
  `cost_level` INT NULL COMMENT '成本/价格等级',
  PRIMARY KEY (`id`),
  KEY `idx_place_destination_id` (`destination_id`),
  KEY `idx_place_name` (`name`),
  KEY `idx_place_type` (`place_type`),
  CONSTRAINT `fk_place_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='场所/建筑表';

CREATE TABLE IF NOT EXISTS `facility` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `destination_id` BIGINT NOT NULL COMMENT '所属目的地 ID',
  `place_id` BIGINT NULL COMMENT '所属场所 ID，可为空',
  `name` VARCHAR(100) NOT NULL COMMENT '设施名称',
  `facility_type` VARCHAR(50) NOT NULL COMMENT '设施类型，如厕所/食堂/商店',
  `description` TEXT NULL COMMENT '设施描述',
  `address` VARCHAR(255) NULL COMMENT '设施地址或位置描述',
  `tel` VARCHAR(50) NULL COMMENT '联系电话',
  `cover_url` VARCHAR(255) NULL COMMENT '封面图地址',
  `lng` DECIMAL(10,6) NULL COMMENT '经度',
  `lat` DECIMAL(10,6) NULL COMMENT '纬度',
  `status` TINYINT NULL DEFAULT 1 COMMENT '状态',
  PRIMARY KEY (`id`),
  KEY `idx_facility_destination_id` (`destination_id`),
  KEY `idx_facility_place_id` (`place_id`),
  KEY `idx_facility_type` (`facility_type`),
  KEY `idx_facility_name` (`name`),
  CONSTRAINT `fk_facility_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_facility_place`
    FOREIGN KEY (`place_id`) REFERENCES `place` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='服务设施表';

CREATE TABLE IF NOT EXISTS `map_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `destination_id` BIGINT NOT NULL COMMENT '所属目的地 ID',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `node_type` VARCHAR(50) NOT NULL COMMENT '节点类型，如 intersection/place/facility',
  `ref_id` BIGINT NULL COMMENT '对应场所或设施 ID',
  `lng` DECIMAL(10,6) NULL COMMENT '经度',
  `lat` DECIMAL(10,6) NULL COMMENT '纬度',
  `floor_no` INT NULL COMMENT '楼层号',
  PRIMARY KEY (`id`),
  KEY `idx_map_node_destination_id` (`destination_id`),
  KEY `idx_map_node_type_ref` (`node_type`, `ref_id`),
  CONSTRAINT `fk_map_node_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地图节点表';

CREATE TABLE IF NOT EXISTS `map_edge` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `destination_id` BIGINT NOT NULL COMMENT '所属目的地 ID',
  `from_node_id` BIGINT NOT NULL COMMENT '起始节点 ID',
  `to_node_id` BIGINT NOT NULL COMMENT '终止节点 ID',
  `distance` DECIMAL(8,2) NOT NULL COMMENT '边长度',
  `ideal_speed` DECIMAL(5,2) NULL COMMENT '理想速度',
  `crowd_factor` DECIMAL(3,2) NULL COMMENT '拥挤度系数',
  `transport_type` VARCHAR(20) NULL COMMENT '交通方式，如 walk/bike/cart',
  `edge_type` VARCHAR(20) NULL COMMENT '边类型，如 road/stairs/elevator',
  `bidirectional_flag` TINYINT NULL DEFAULT 0 COMMENT '是否双向',
  PRIMARY KEY (`id`),
  KEY `idx_map_edge_destination_id` (`destination_id`),
  KEY `idx_map_edge_from_node_id` (`from_node_id`),
  KEY `idx_map_edge_to_node_id` (`to_node_id`),
  KEY `idx_map_edge_transport_type` (`transport_type`),
  CONSTRAINT `fk_map_edge_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_map_edge_from_node`
    FOREIGN KEY (`from_node_id`) REFERENCES `map_node` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_map_edge_to_node`
    FOREIGN KEY (`to_node_id`) REFERENCES `map_node` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地图边表';

CREATE TABLE IF NOT EXISTS `route_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `destination_id` BIGINT NOT NULL COMMENT '目的地 ID',
  `start_node_id` BIGINT NULL COMMENT '起点节点 ID',
  `end_node_id` BIGINT NULL COMMENT '终点节点 ID',
  `path_node_json` TEXT NULL COMMENT '路径节点序列 JSON',
  `path_edge_json` TEXT NULL COMMENT '路径边序列 JSON',
  `strategy_type` VARCHAR(50) NULL COMMENT '路径策略类型',
  `transport_type` VARCHAR(20) NULL COMMENT '交通方式',
  `total_distance` DECIMAL(10,2) NULL COMMENT '总距离',
  `estimated_time` INT NULL COMMENT '预计时间，按分钟存储',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_route_history_user_id` (`user_id`),
  KEY `idx_route_history_destination_id` (`destination_id`),
  KEY `idx_route_history_created_at` (`created_at`),
  KEY `idx_route_history_start_node_id` (`start_node_id`),
  KEY `idx_route_history_end_node_id` (`end_node_id`),
  CONSTRAINT `fk_route_history_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_route_history_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_route_history_start_node`
    FOREIGN KEY (`start_node_id`) REFERENCES `map_node` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL,
  CONSTRAINT `fk_route_history_end_node`
    FOREIGN KEY (`end_node_id`) REFERENCES `map_node` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线记录表';

CREATE TABLE IF NOT EXISTS `diary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '作者用户 ID',
  `destination_id` BIGINT NOT NULL COMMENT '关联目的地 ID',
  `route_history_id` BIGINT NULL COMMENT '关联路线记录 ID，可为空',
  `title` VARCHAR(150) NOT NULL COMMENT '日记标题',
  `content_text` LONGTEXT NOT NULL COMMENT '日记正文',
  `content_compressed` LONGBLOB NULL COMMENT '压缩内容，扩展字段',
  `heat_score` DECIMAL(5,2) NULL DEFAULT 0 COMMENT '热度分',
  `rating_score` DECIMAL(3,2) NULL DEFAULT 0 COMMENT '平均评分',
  `rating_count` INT NOT NULL DEFAULT 0 COMMENT '评分人数',
  `visibility` VARCHAR(20) NULL DEFAULT 'public' COMMENT '可见性，如 public/private',
  `status` TINYINT NULL DEFAULT 1 COMMENT '状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_diary_user_id` (`user_id`),
  KEY `idx_diary_destination_id` (`destination_id`),
  KEY `idx_diary_route_history_id` (`route_history_id`),
  KEY `idx_diary_title` (`title`),
  KEY `idx_diary_heat_rating` (`heat_score`, `rating_score`),
  CONSTRAINT `fk_diary_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_diary_destination`
    FOREIGN KEY (`destination_id`) REFERENCES `destination` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT `fk_diary_route_history`
    FOREIGN KEY (`route_history_id`) REFERENCES `route_history` (`id`)
    ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='旅游日记表';

CREATE TABLE IF NOT EXISTS `diary_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `diary_id` BIGINT NOT NULL COMMENT '所属日记 ID',
  `media_type` VARCHAR(20) NOT NULL COMMENT '媒体类型，image/video',
  `file_url` VARCHAR(255) NOT NULL COMMENT '文件访问地址',
  `file_name` VARCHAR(100) NULL COMMENT '文件名',
  `sort_no` INT NULL DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (`id`),
  KEY `idx_diary_media_diary_id` (`diary_id`),
  CONSTRAINT `fk_diary_media_diary`
    FOREIGN KEY (`diary_id`) REFERENCES `diary` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日记媒体表';

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
