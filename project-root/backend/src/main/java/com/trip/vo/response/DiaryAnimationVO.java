package com.trip.vo.response;

import com.trip.entity.DiaryAnimation;
import com.trip.model.ai.AnimationScript;
import java.time.LocalDateTime;

public class DiaryAnimationVO {

    private Long id;
    private Long diaryId;
    private String provider;
    private String title;
    private String narration;
    private String status;
    private AnimationScriptVO script;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DiaryAnimationVO from(DiaryAnimation animation, AnimationScript script) {
        DiaryAnimationVO vo = new DiaryAnimationVO();
        vo.setId(animation.getId());
        vo.setDiaryId(animation.getDiaryId());
        vo.setProvider(animation.getProvider());
        vo.setTitle(animation.getAnimationTitle());
        vo.setNarration(animation.getNarrationText());
        vo.setStatus(animation.getStatus());
        vo.setScript(AnimationScriptVO.from(script));
        vo.setCreatedAt(animation.getCreatedAt());
        vo.setUpdatedAt(animation.getUpdatedAt());
        return vo;
    }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AnimationScriptVO getScript() {
        return script;
    }

    public void setScript(AnimationScriptVO script) {
        this.script = script;
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
