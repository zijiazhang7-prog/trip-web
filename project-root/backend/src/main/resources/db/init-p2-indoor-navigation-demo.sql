-- 室内导航演示数据：北邮沙河教学楼与颐和园园史博物馆。
-- 前置：已执行 init-p0-schema.sql 和 migrate-p2-indoor-navigation-schema.sql。
-- 本脚本仅重建上述两栋演示建筑的室内节点和边，可重复执行。

USE `tour_system`;

INSERT INTO `destination`
  (`name`, `type`, `category`, `city`, `description`, `heat_score`, `rating_score`, `status`)
SELECT
  '北京邮电大学沙河校区', 'campus', '高校校园', '北京',
  '室内导航演示校园。', 80, 4.8, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `destination` WHERE `name` = '北京邮电大学沙河校区'
);

INSERT INTO `destination`
  (`name`, `type`, `category`, `city`, `description`, `heat_score`, `rating_score`, `status`)
SELECT
  '颐和园', 'scenic', '历史文化', '北京',
  '室内导航演示景区。', 95, 4.9, 1
WHERE NOT EXISTS (
  SELECT 1 FROM `destination` WHERE `name` = '颐和园'
);

SET @campus_destination_id = (
  SELECT MIN(`id`) FROM `destination` WHERE `name` = '北京邮电大学沙河校区'
);
SET @scenic_destination_id = (
  SELECT MIN(`id`) FROM `destination` WHERE `name` = '颐和园'
);

INSERT INTO `place`
  (`destination_id`, `name`, `place_type`, `description`, `floor_info`,
   `heat_score`, `rating_score`, `suggested_duration_min`, `cost_level`)
SELECT
  @campus_destination_id, '教学楼A', '教学楼',
  '三层教学楼室内导航演示建筑。', '1F-3F', 70, 4.7, 45, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `place`
  WHERE `destination_id` = @campus_destination_id AND `name` = '教学楼A'
);

INSERT INTO `place`
  (`destination_id`, `name`, `place_type`, `description`, `floor_info`,
   `heat_score`, `rating_score`, `suggested_duration_min`, `cost_level`)
SELECT
  @scenic_destination_id, '园史博物馆', '博物馆',
  '三层景区博物馆室内导航演示建筑。', '1F-3F', 75, 4.8, 60, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `place`
  WHERE `destination_id` = @scenic_destination_id AND `name` = '园史博物馆'
);

SET @campus_building_id = (
  SELECT MIN(`id`) FROM `place`
  WHERE `destination_id` = @campus_destination_id AND `name` = '教学楼A'
);
SET @museum_building_id = (
  SELECT MIN(`id`) FROM `place`
  WHERE `destination_id` = @scenic_destination_id AND `name` = '园史博物馆'
);

DELETE edge
FROM `map_edge` edge
JOIN `map_node` node
  ON node.`id` = edge.`from_node_id` OR node.`id` = edge.`to_node_id`
WHERE node.`place_id` IN (@campus_building_id, @museum_building_id);

DELETE FROM `map_node`
WHERE `place_id` IN (@campus_building_id, @museum_building_id);

INSERT INTO `map_node`
  (`destination_id`, `place_id`, `node_name`, `node_type`, `floor_no`, `indoor_x`, `indoor_y`)
VALUES
  (@campus_destination_id, @campus_building_id, '教学楼A大门', 'gate', 1, 80, 300),
  (@campus_destination_id, @campus_building_id, '一层大厅', 'hall', 1, 190, 300),
  (@campus_destination_id, @campus_building_id, '一层电梯口', 'elevator', 1, 320, 220),
  (@campus_destination_id, @campus_building_id, '一层楼梯口', 'stair', 1, 320, 380),
  (@campus_destination_id, @campus_building_id, '一层走廊', 'corridor', 1, 470, 300),
  (@campus_destination_id, @campus_building_id, '二层电梯口', 'elevator', 2, 320, 220),
  (@campus_destination_id, @campus_building_id, '二层楼梯口', 'stair', 2, 320, 380),
  (@campus_destination_id, @campus_building_id, '二层走廊', 'corridor', 2, 470, 300),
  (@campus_destination_id, @campus_building_id, '201教室', 'room', 2, 640, 220),
  (@campus_destination_id, @campus_building_id, '202教室', 'room', 2, 640, 380),
  (@campus_destination_id, @campus_building_id, '三层电梯口', 'elevator', 3, 320, 220),
  (@campus_destination_id, @campus_building_id, '三层楼梯口', 'stair', 3, 320, 380),
  (@campus_destination_id, @campus_building_id, '三层走廊', 'corridor', 3, 470, 300),
  (@campus_destination_id, @campus_building_id, '301教室', 'room', 3, 640, 220),
  (@campus_destination_id, @campus_building_id, '302教室', 'room', 3, 640, 380),
  (@scenic_destination_id, @museum_building_id, '博物馆入口', 'gate', 1, 80, 300),
  (@scenic_destination_id, @museum_building_id, '一层序厅', 'hall', 1, 190, 300),
  (@scenic_destination_id, @museum_building_id, '一层电梯口', 'elevator', 1, 320, 220),
  (@scenic_destination_id, @museum_building_id, '一层楼梯口', 'stair', 1, 320, 380),
  (@scenic_destination_id, @museum_building_id, '一层展廊', 'corridor', 1, 470, 300),
  (@scenic_destination_id, @museum_building_id, '二层电梯口', 'elevator', 2, 320, 220),
  (@scenic_destination_id, @museum_building_id, '二层楼梯口', 'stair', 2, 320, 380),
  (@scenic_destination_id, @museum_building_id, '二层展廊', 'corridor', 2, 470, 300),
  (@scenic_destination_id, @museum_building_id, '历史展厅', 'room', 2, 640, 220),
  (@scenic_destination_id, @museum_building_id, '文创展厅', 'room', 2, 640, 380),
  (@scenic_destination_id, @museum_building_id, '三层电梯口', 'elevator', 3, 320, 220),
  (@scenic_destination_id, @museum_building_id, '三层楼梯口', 'stair', 3, 320, 380),
  (@scenic_destination_id, @museum_building_id, '三层展廊', 'corridor', 3, 470, 300),
  (@scenic_destination_id, @museum_building_id, '数字展厅', 'room', 3, 640, 220),
  (@scenic_destination_id, @museum_building_id, '观景展厅', 'room', 3, 640, 380);

DELIMITER //

DROP PROCEDURE IF EXISTS add_indoor_demo_edge//

CREATE PROCEDURE add_indoor_demo_edge(
  IN p_destination_id BIGINT,
  IN p_building_id BIGINT,
  IN p_from_name VARCHAR(100),
  IN p_to_name VARCHAR(100),
  IN p_distance DECIMAL(8,2),
  IN p_ideal_speed DECIMAL(5,2),
  IN p_edge_type VARCHAR(20)
)
BEGIN
  INSERT INTO `map_edge`
    (`destination_id`, `from_node_id`, `to_node_id`, `distance`,
     `ideal_speed`, `crowd_factor`, `transport_type`, `edge_type`, `bidirectional_flag`)
  SELECT
    p_destination_id, from_node.`id`, to_node.`id`, p_distance,
    p_ideal_speed, 1.00, 'walk', p_edge_type, 1
  FROM `map_node` from_node
  JOIN `map_node` to_node
    ON to_node.`place_id` = p_building_id AND to_node.`node_name` = p_to_name
  WHERE from_node.`place_id` = p_building_id
    AND from_node.`node_name` = p_from_name;
END//

CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '教学楼A大门', '一层大厅', 18, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '一层大厅', '一层电梯口', 15, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '一层大厅', '一层楼梯口', 20, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '一层大厅', '一层走廊', 25, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '一层电梯口', '二层电梯口', 12, 24, 'elevator')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '二层电梯口', '三层电梯口', 12, 24, 'elevator')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '一层楼梯口', '二层楼梯口', 18, 18, 'stair')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '二层楼梯口', '三层楼梯口', 18, 18, 'stair')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '二层电梯口', '二层走廊', 15, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '二层楼梯口', '二层走廊', 18, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '二层走廊', '201教室', 16, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '二层走廊', '202教室', 18, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '三层电梯口', '三层走廊', 15, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '三层楼梯口', '三层走廊', 18, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '三层走廊', '301教室', 16, 80, 'corridor')//
CALL add_indoor_demo_edge(@campus_destination_id, @campus_building_id, '三层走廊', '302教室', 18, 80, 'corridor')//

CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '博物馆入口', '一层序厅', 20, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '一层序厅', '一层电梯口', 16, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '一层序厅', '一层楼梯口', 22, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '一层序厅', '一层展廊', 28, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '一层电梯口', '二层电梯口', 14, 28, 'elevator')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '二层电梯口', '三层电梯口', 14, 28, 'elevator')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '一层楼梯口', '二层楼梯口', 20, 20, 'stair')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '二层楼梯口', '三层楼梯口', 20, 20, 'stair')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '二层电梯口', '二层展廊', 16, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '二层楼梯口', '二层展廊', 20, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '二层展廊', '历史展厅', 18, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '二层展廊', '文创展厅', 20, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '三层电梯口', '三层展廊', 16, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '三层楼梯口', '三层展廊', 20, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '三层展廊', '数字展厅', 18, 80, 'corridor')//
CALL add_indoor_demo_edge(@scenic_destination_id, @museum_building_id, '三层展廊', '观景展厅', 20, 80, 'corridor')//

DROP PROCEDURE add_indoor_demo_edge//

DELIMITER ;

SELECT
  place.`name` AS `building_name`,
  COUNT(DISTINCT node.`floor_no`) AS `floor_count`,
  COUNT(DISTINCT node.`id`) AS `node_count`,
  COUNT(DISTINCT edge.`id`) AS `edge_count`
FROM `place` place
JOIN `map_node` node ON node.`place_id` = place.`id`
LEFT JOIN `map_edge` edge ON edge.`from_node_id` = node.`id`
WHERE place.`id` IN (@campus_building_id, @museum_building_id)
GROUP BY place.`id`, place.`name`;
