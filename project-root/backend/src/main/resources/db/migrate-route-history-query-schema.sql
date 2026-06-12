-- Route 历史查询增量迁移。
-- 为新生成的多目标路线保存实际目标访问顺序；旧记录保持 NULL。

USE `tour_system`;

SET @ordered_target_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'route_history'
    AND COLUMN_NAME = 'ordered_target_node_json'
);

SET @ordered_target_ddl = IF(
  @ordered_target_column_exists = 0,
  'ALTER TABLE `route_history` ADD COLUMN `ordered_target_node_json` TEXT NULL COMMENT ''多目标实际访问顺序 JSON，单目标可为空'' AFTER `path_edge_json`',
  'SELECT 1'
);

PREPARE ordered_target_statement FROM @ordered_target_ddl;
EXECUTE ordered_target_statement;
DEALLOCATE PREPARE ordered_target_statement;
