-- P1 ImportService database initialization script.
-- Scope: import batch tracking and row-level failure detail only.

USE `tour_system`;

CREATE TABLE IF NOT EXISTS `import_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_name` VARCHAR(100) NOT NULL COMMENT '批次名称',
  `target_table` VARCHAR(50) NOT NULL COMMENT '目标表名',
  `source_type` VARCHAR(20) NOT NULL COMMENT '来源类型，如 csv/json',
  `file_name` VARCHAR(255) NOT NULL COMMENT '导入文件名',
  `file_size` BIGINT NOT NULL COMMENT '导入文件大小，单位字节',
  `status` VARCHAR(30) NOT NULL COMMENT '导入状态',
  `total_rows` BIGINT NOT NULL DEFAULT 0 COMMENT '总行数',
  `success_rows` BIGINT NOT NULL DEFAULT 0 COMMENT '成功行数',
  `failed_rows` BIGINT NOT NULL DEFAULT 0 COMMENT '失败行数',
  `error_message` VARCHAR(500) NULL COMMENT '首条错误摘要',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_import_batch_target_table` (`target_table`),
  KEY `idx_import_batch_status` (`status`),
  KEY `idx_import_batch_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入批次表';

CREATE TABLE IF NOT EXISTS `import_failure` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '所属导入批次 ID',
  `row_no` INT NOT NULL COMMENT '源文件行号或 JSON 数组序号',
  `field_name` VARCHAR(100) NULL COMMENT '失败字段名',
  `error_message` VARCHAR(500) NOT NULL COMMENT '错误信息',
  `raw_data_json` TEXT NULL COMMENT '原始行数据 JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_import_failure_batch_id` (`batch_id`),
  KEY `idx_import_failure_row_no` (`row_no`),
  CONSTRAINT `fk_import_failure_batch`
    FOREIGN KEY (`batch_id`) REFERENCES `import_batch` (`id`)
    ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导入失败明细表';
