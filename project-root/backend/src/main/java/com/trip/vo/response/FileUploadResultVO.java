package com.trip.vo.response;

/**
 * 文件上传结果。
 */
public class FileUploadResultVO {

    private String bizType;
    private String fileName;
    private String fileUrl;

    public FileUploadResultVO() {
    }

    public FileUploadResultVO(String bizType, String fileName, String fileUrl) {
        this.bizType = bizType;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}
