package com.trip.service;

import com.trip.service.model.CompressionMaintenanceResult;

/**
 * 负责历史日记压缩数据的回填、校验和修复。
 */
public interface CompressionMaintenanceService {

    CompressionMaintenanceResult maintain();
}
