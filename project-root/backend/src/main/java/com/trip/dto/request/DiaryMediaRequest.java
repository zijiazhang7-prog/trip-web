package com.trip.dto.request;

import jakarta.validation.constraints.Size;

public class DiaryMediaRequest {

    @Size(max = 20)
    private String mediaType;

    @Size(max = 255)
    private String fileUrl;

    @Size(max = 100)
    private String fileName;

    private Integer sortNo;

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
