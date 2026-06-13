-- 室内导航最小字段迁移。
-- 执行前请确认当前数据库为 tour_system，且 map_node 尚未包含以下字段和约束。

ALTER TABLE `map_node`
  ADD COLUMN `place_id` BIGINT NULL COMMENT '室内节点所属建筑，对应 place.id' AFTER `ref_id`,
  ADD COLUMN `indoor_x` DECIMAL(8,2) NULL COMMENT '室内楼层图归一化 X 坐标' AFTER `floor_no`,
  ADD COLUMN `indoor_y` DECIMAL(8,2) NULL COMMENT '室内楼层图归一化 Y 坐标' AFTER `indoor_x`,
  ADD KEY `idx_map_node_indoor` (`destination_id`, `place_id`, `floor_no`),
  ADD CONSTRAINT `fk_map_node_place`
    FOREIGN KEY (`place_id`) REFERENCES `place` (`id`)
    ON UPDATE CASCADE ON DELETE RESTRICT;
