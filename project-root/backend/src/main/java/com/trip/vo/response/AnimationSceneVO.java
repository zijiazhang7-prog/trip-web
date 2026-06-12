package com.trip.vo.response;

import com.trip.model.ai.AnimationScene;

public class AnimationSceneVO {

    private int order;
    private Long mediaId;
    private String fileUrl;
    private String visualDescription;
    private int durationMs;
    private String motion;
    private String transition;
    private String subtitle;
    private String narration;

    public static AnimationSceneVO from(AnimationScene scene) {
        AnimationSceneVO vo = new AnimationSceneVO();
        vo.setOrder(scene.order());
        vo.setMediaId(scene.mediaId());
        vo.setFileUrl(scene.fileUrl());
        vo.setVisualDescription(scene.visualDescription());
        vo.setDurationMs(scene.durationMs());
        vo.setMotion(scene.motion());
        vo.setTransition(scene.transition());
        vo.setSubtitle(scene.subtitle());
        vo.setNarration(scene.narration());
        return vo;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getVisualDescription() {
        return visualDescription;
    }

    public void setVisualDescription(String visualDescription) {
        this.visualDescription = visualDescription;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public String getMotion() {
        return motion;
    }

    public void setMotion(String motion) {
        this.motion = motion;
    }

    public String getTransition() {
        return transition;
    }

    public void setTransition(String transition) {
        this.transition = transition;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }
}
