-- 将日记热度统一为浏览次数。
-- 执行前请确认已备份 tour_system.diary。

UPDATE `diary`
SET `heat_score` = GREATEST(ROUND(COALESCE(`heat_score`, 0)), 0);

ALTER TABLE `diary`
  MODIFY COLUMN `heat_score` BIGINT UNSIGNED NOT NULL DEFAULT 0
  COMMENT '浏览量，即日记热度';
