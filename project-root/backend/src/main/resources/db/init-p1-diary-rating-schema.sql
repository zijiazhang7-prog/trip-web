-- Diary rating P1 schema and migration.
-- Safe to run repeatedly against an existing tour_system database.

USE `tour_system`;

DELIMITER //

DROP PROCEDURE IF EXISTS add_diary_rating_count_if_missing//

CREATE PROCEDURE add_diary_rating_count_if_missing()
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'diary'
      AND column_name = 'rating_count'
  ) THEN
    ALTER TABLE `diary`
      ADD COLUMN `rating_count` INT NOT NULL DEFAULT 0 COMMENT '评分人数'
      AFTER `rating_score`;
  END IF;
END//

DELIMITER ;

CALL add_diary_rating_count_if_missing();
DROP PROCEDURE add_diary_rating_count_if_missing;

CREATE TABLE IF NOT EXISTS `diary_rating` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `diary_id` BIGINT NOT NULL COMMENT '日记 ID',
  `user_id` BIGINT NOT NULL COMMENT '评分用户 ID',
  `score` TINYINT NOT NULL COMMENT '评分，1-5',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_diary_rating_user` (`diary_id`, `user_id`),
  KEY `idx_diary_rating_user_id` (`user_id`),
  CONSTRAINT `fk_diary_rating_diary`
    FOREIGN KEY (`diary_id`) REFERENCES `diary` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `fk_diary_rating_user`
    FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT `chk_diary_rating_score` CHECK (`score` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='日记评分表';

UPDATE `diary` d
LEFT JOIN (
  SELECT `diary_id`, ROUND(AVG(`score`), 2) AS `rating_score`, COUNT(*) AS `rating_count`
  FROM `diary_rating`
  GROUP BY `diary_id`
) r ON r.`diary_id` = d.`id`
SET d.`rating_score` = COALESCE(r.`rating_score`, 0),
    d.`rating_count` = COALESCE(r.`rating_count`, 0);
