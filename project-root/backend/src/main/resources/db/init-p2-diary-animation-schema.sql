-- Diary animation P2 schema.
-- Safe to run repeatedly against an existing tour_system database.

USE `tour_system`;

CREATE TABLE IF NOT EXISTS `diary_animation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `diary_id` BIGINT NOT NULL COMMENT '关联日记 ID',
  `provider` VARCHAR(50) NOT NULL DEFAULT 'mock-template' COMMENT '脚本生成提供方',
  `animation_title` VARCHAR(150) NOT NULL COMMENT '动画标题',
  `narration_text` TEXT NOT NULL COMMENT '动画总旁白',
  `script_json` JSON NOT NULL COMMENT '前端可播放动画脚本',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ready' COMMENT '状态，当前为 ready',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_diary_animation_diary_id` (`diary_id`),
  CONSTRAINT `fk_diary_animation_diary`
    FOREIGN KEY (`diary_id`) REFERENCES `diary` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日记 AIGC 动画脚本表';
