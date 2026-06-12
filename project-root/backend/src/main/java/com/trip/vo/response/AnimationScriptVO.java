package com.trip.vo.response;

import com.trip.model.ai.AnimationScript;
import java.util.List;

public class AnimationScriptVO {

    private String schemaVersion;
    private String aspectRatio;
    private int totalDurationMs;
    private String backgroundMusic;
    private List<AnimationSceneVO> scenes;

    public static AnimationScriptVO from(AnimationScript script) {
        AnimationScriptVO vo = new AnimationScriptVO();
        vo.setSchemaVersion(script.schemaVersion());
        vo.setAspectRatio(script.aspectRatio());
        vo.setTotalDurationMs(script.totalDurationMs());
        vo.setBackgroundMusic(script.backgroundMusic());
        vo.setScenes(script.scenes().stream().map(AnimationSceneVO::from).toList());
        return vo;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(String aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public int getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(int totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public String getBackgroundMusic() {
        return backgroundMusic;
    }

    public void setBackgroundMusic(String backgroundMusic) {
        this.backgroundMusic = backgroundMusic;
    }

    public List<AnimationSceneVO> getScenes() {
        return scenes;
    }

    public void setScenes(List<AnimationSceneVO> scenes) {
        this.scenes = scenes;
    }
}
