package com.trip.vo.response;

import com.trip.entity.ImportFailure;
import java.time.LocalDateTime;

/**
 * 管理端导入失败明细响应对象。
 */
public class AdminImportFailureVO {

    private Long id;
    private Long batchId;
    private Integer rowNo;
    private String fieldName;
    private String errorMessage;
    private String rawDataJson;
    private LocalDateTime createdAt;

    public static AdminImportFailureVO from(ImportFailure failure) {
        if (failure == null) {
            return null;
        }
        AdminImportFailureVO vo = new AdminImportFailureVO();
        vo.setId(failure.getId());
        vo.setBatchId(failure.getBatchId());
        vo.setRowNo(failure.getRowNo());
        vo.setFieldName(failure.getFieldName());
        vo.setErrorMessage(failure.getErrorMessage());
        vo.setRawDataJson(failure.getRawDataJson());
        vo.setCreatedAt(failure.getCreatedAt());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRawDataJson() {
        return rawDataJson;
    }

    public void setRawDataJson(String rawDataJson) {
        this.rawDataJson = rawDataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
