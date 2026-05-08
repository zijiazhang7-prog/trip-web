package com.trip.vo.response;

import com.trip.entity.DiaryMedia;

public class DiaryMediaVO {

    private Long id;
    private String mediaType;
    private String fileUrl;
    private String fileName;
    private Integer sortNo;

    public static DiaryMediaVO from(DiaryMedia media) {
        DiaryMediaVO vo = new DiaryMediaVO();
        vo.setId(media.getId());
        vo.setMediaType(media.getMediaType());
        vo.setFileUrl(media.getFileUrl());
        vo.setFileName(media.getFileName());
        vo.setSortNo(media.getSortNo());
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
