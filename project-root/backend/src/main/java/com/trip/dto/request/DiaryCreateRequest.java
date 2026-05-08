package com.trip.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class DiaryCreateRequest {

    @NotNull
    private Long destinationId;

    private Long routeHistoryId;

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String contentText;

    @Size(max = 20)
    private String visibility;

    @Valid
    @Size(max = 9)
    private List<DiaryMediaRequest> mediaList;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getRouteHistoryId() {
        return routeHistoryId;
    }

    public void setRouteHistoryId(Long routeHistoryId) {
        this.routeHistoryId = routeHistoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<DiaryMediaRequest> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<DiaryMediaRequest> mediaList) {
        this.mediaList = mediaList;
    }
}
