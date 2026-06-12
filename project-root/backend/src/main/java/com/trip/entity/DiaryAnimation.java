package com.trip.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("diary_animation")
public class DiaryAnimation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long diaryId;

    private String provider;

    private String animationTitle;

    private String narrationText;

    private String scriptJson;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(Long diaryId) {
        this.diaryId = diaryId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAnimationTitle() {
        return animationTitle;
    }

    public void setAnimationTitle(String animationTitle) {
        this.animationTitle = animationTitle;
    }

    public String getNarrationText() {
        return narrationText;
    }

    public void setNarrationText(String narrationText) {
        this.narrationText = narrationText;
    }

    public String getScriptJson() {
        return scriptJson;
    }

    public void setScriptJson(String scriptJson) {
        this.scriptJson = scriptJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
