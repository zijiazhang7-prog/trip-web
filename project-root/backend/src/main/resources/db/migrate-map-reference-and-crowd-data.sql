-- 地图节点业务引用与道路拥挤度演示数据修复。
-- 适用数据库：MySQL 8 / tour_system
--
-- 设计约束：
-- 1. 仅填充 ref_id 为空的 place / facility 节点，不覆盖已有映射。
-- 2. place 采用“同目的地、名称唯一精确匹配”或“同目的地、同坐标、唯一候选”。
-- 3. facility 采用“同目的地、名称唯一精确匹配”或“同目的地、同坐标、唯一候选”。
-- 4. 两种 facility 规则冲突时不写入，避免错误关联。
-- 5. crowd_factor 为课程演示用静态模拟系数，不代表实时拥堵数据。
-- 6. 拥挤度由道路端点稳定生成，重复执行结果一致。

USE `tour_system`;

-- 执行前基线。
SELECT
  `node_type`,
  COUNT(*) AS `node_count`,
  SUM(`ref_id` IS NULL) AS `null_ref_count`
FROM `map_node`
WHERE `node_type` IN ('place', 'facility')
GROUP BY `node_type`;

SELECT
  COUNT(*) AS `edge_count`,
  MIN(`crowd_factor`) AS `min_crowd_factor`,
  MAX(`crowd_factor`) AS `max_crowd_factor`,
  COUNT(DISTINCT `crowd_factor`) AS `distinct_crowd_factor_count`
FROM `map_edge`;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_place_name_ref`;
CREATE TEMPORARY TABLE `tmp_place_name_ref` (
  `node_id` BIGINT NOT NULL PRIMARY KEY,
  `ref_id` BIGINT NOT NULL
) ENGINE = InnoDB;

INSERT INTO `tmp_place_name_ref` (`node_id`, `ref_id`)
SELECT
  `n`.`id`,
  MIN(`p`.`id`)
FROM `map_node` AS `n`
JOIN `place` AS `p`
  ON `p`.`destination_id` = `n`.`destination_id`
 AND TRIM(`p`.`name`) = TRIM(`n`.`node_name`)
WHERE `n`.`node_type` = 'place'
  AND `n`.`ref_id` IS NULL
GROUP BY `n`.`id`
HAVING COUNT(*) = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_place_coordinate_ref`;
CREATE TEMPORARY TABLE `tmp_place_coordinate_ref` (
  `node_id` BIGINT NOT NULL PRIMARY KEY,
  `ref_id` BIGINT NOT NULL
) ENGINE = InnoDB;

INSERT INTO `tmp_place_coordinate_ref` (`node_id`, `ref_id`)
SELECT
  `n`.`id`,
  MIN(`p`.`id`)
FROM `map_node` AS `n`
JOIN `place` AS `p`
  ON `p`.`destination_id` = `n`.`destination_id`
 AND `p`.`lng` = `n`.`lng`
 AND `p`.`lat` = `n`.`lat`
WHERE `n`.`node_type` = 'place'
  AND `n`.`ref_id` IS NULL
  AND `n`.`lng` IS NOT NULL
  AND `n`.`lat` IS NOT NULL
GROUP BY `n`.`id`
HAVING COUNT(*) = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_place_node_ref`;
CREATE TEMPORARY TABLE `tmp_place_node_ref` (
  `node_id` BIGINT NOT NULL PRIMARY KEY,
  `ref_id` BIGINT NOT NULL
) ENGINE = InnoDB;

INSERT INTO `tmp_place_node_ref` (`node_id`, `ref_id`)
SELECT
  `n`.`id`,
  COALESCE(`name_ref`.`ref_id`, `coordinate_ref`.`ref_id`)
FROM `map_node` AS `n`
LEFT JOIN `tmp_place_name_ref` AS `name_ref`
  ON `name_ref`.`node_id` = `n`.`id`
LEFT JOIN `tmp_place_coordinate_ref` AS `coordinate_ref`
  ON `coordinate_ref`.`node_id` = `n`.`id`
WHERE `n`.`node_type` = 'place'
  AND `n`.`ref_id` IS NULL
  AND (`name_ref`.`ref_id` IS NOT NULL OR `coordinate_ref`.`ref_id` IS NOT NULL)
  AND (
    `name_ref`.`ref_id` IS NULL
    OR `coordinate_ref`.`ref_id` IS NULL
    OR `name_ref`.`ref_id` = `coordinate_ref`.`ref_id`
  );

UPDATE `map_node` AS `n`
JOIN `tmp_place_node_ref` AS `m`
  ON `m`.`node_id` = `n`.`id`
SET `n`.`ref_id` = `m`.`ref_id`
WHERE `n`.`ref_id` IS NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_facility_name_ref`;
CREATE TEMPORARY TABLE `tmp_facility_name_ref` (
  `node_id` BIGINT NOT NULL PRIMARY KEY,
  `ref_id` BIGINT NOT NULL
) ENGINE = InnoDB;

INSERT INTO `tmp_facility_name_ref` (`node_id`, `ref_id`)
SELECT
  `n`.`id`,
  MIN(`f`.`id`)
FROM `map_node` AS `n`
JOIN `facility` AS `f`
  ON `f`.`destination_id` = `n`.`destination_id`
 AND TRIM(`f`.`name`) = TRIM(`n`.`node_name`)
WHERE `n`.`node_type` = 'facility'
  AND `n`.`ref_id` IS NULL
GROUP BY `n`.`id`
HAVING COUNT(*) = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_facility_coordinate_ref`;
CREATE TEMPORARY TABLE `tmp_facility_coordinate_ref` (
  `node_id` BIGINT NOT NULL PRIMARY KEY,
  `ref_id` BIGINT NOT NULL
) ENGINE = InnoDB;

INSERT INTO `tmp_facility_coordinate_ref` (`node_id`, `ref_id`)
SELECT
  `n`.`id`,
  MIN(`f`.`id`)
FROM `map_node` AS `n`
JOIN `facility` AS `f`
  ON `f`.`destination_id` = `n`.`destination_id`
 AND `f`.`lng` = `n`.`lng`
 AND `f`.`lat` = `n`.`lat`
WHERE `n`.`node_type` = 'facility'
  AND `n`.`ref_id` IS NULL
  AND `n`.`lng` IS NOT NULL
  AND `n`.`lat` IS NOT NULL
GROUP BY `n`.`id`
HAVING COUNT(*) = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_facility_node_ref`;
CREATE TEMPORARY TABLE `tmp_facility_node_ref` (
  `node_id` BIGINT NOT NULL PRIMARY KEY,
  `ref_id` BIGINT NOT NULL
) ENGINE = InnoDB;

INSERT INTO `tmp_facility_node_ref` (`node_id`, `ref_id`)
SELECT
  `n`.`id`,
  COALESCE(`name_ref`.`ref_id`, `coordinate_ref`.`ref_id`)
FROM `map_node` AS `n`
LEFT JOIN `tmp_facility_name_ref` AS `name_ref`
  ON `name_ref`.`node_id` = `n`.`id`
LEFT JOIN `tmp_facility_coordinate_ref` AS `coordinate_ref`
  ON `coordinate_ref`.`node_id` = `n`.`id`
WHERE `n`.`node_type` = 'facility'
  AND `n`.`ref_id` IS NULL
  AND (`name_ref`.`ref_id` IS NOT NULL OR `coordinate_ref`.`ref_id` IS NOT NULL)
  AND (
    `name_ref`.`ref_id` IS NULL
    OR `coordinate_ref`.`ref_id` IS NULL
    OR `name_ref`.`ref_id` = `coordinate_ref`.`ref_id`
  );

UPDATE `map_node` AS `n`
JOIN `tmp_facility_node_ref` AS `m`
  ON `m`.`node_id` = `n`.`id`
SET `n`.`ref_id` = `m`.`ref_id`
WHERE `n`.`ref_id` IS NULL;

-- DECIMAL(3,2) 下生成 0.55～1.00 共 46 档稳定系数。
-- LEAST/GREATEST 保证同一物理道路的双向边得到相同系数。
UPDATE `map_edge`
SET `crowd_factor` = CAST(
  ROUND(
    0.55 + MOD(
      CRC32(CONCAT(
        `destination_id`,
        ':',
        LEAST(`from_node_id`, `to_node_id`),
        ':',
        GREATEST(`from_node_id`, `to_node_id`)
      )),
      46
    ) / 100,
    2
  ) AS DECIMAL(3, 2)
);

COMMIT;

-- 执行后校验。
SELECT
  `node_type`,
  COUNT(*) AS `node_count`,
  SUM(`ref_id` IS NOT NULL) AS `linked_count`,
  SUM(`ref_id` IS NULL) AS `unlinked_count`
FROM `map_node`
WHERE `node_type` IN ('place', 'facility')
GROUP BY `node_type`;

SELECT COUNT(*) AS `invalid_place_ref_count`
FROM `map_node` AS `n`
LEFT JOIN `place` AS `p`
  ON `p`.`id` = `n`.`ref_id`
 AND `p`.`destination_id` = `n`.`destination_id`
WHERE `n`.`node_type` = 'place'
  AND `n`.`ref_id` IS NOT NULL
  AND `p`.`id` IS NULL;

SELECT COUNT(*) AS `invalid_facility_ref_count`
FROM `map_node` AS `n`
LEFT JOIN `facility` AS `f`
  ON `f`.`id` = `n`.`ref_id`
 AND `f`.`destination_id` = `n`.`destination_id`
WHERE `n`.`node_type` = 'facility'
  AND `n`.`ref_id` IS NOT NULL
  AND `f`.`id` IS NULL;

SELECT
  COUNT(*) AS `edge_count`,
  MIN(`crowd_factor`) AS `min_crowd_factor`,
  MAX(`crowd_factor`) AS `max_crowd_factor`,
  COUNT(DISTINCT `crowd_factor`) AS `distinct_crowd_factor_count`,
  SUM(`crowd_factor` IS NULL OR `crowd_factor` <= 0 OR `crowd_factor` > 1) AS `invalid_crowd_factor_count`
FROM `map_edge`;

-- 未能高置信关联的设施节点，需补充 facility 数据或人工维护映射。
SELECT
  `id`,
  `destination_id`,
  `node_name`,
  `lng`,
  `lat`
FROM `map_node`
WHERE `node_type` = 'facility'
  AND `ref_id` IS NULL
ORDER BY `destination_id`, `id`;
