package com.trip.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 室内单目标路线规划请求。
 */
public class IndoorRoutePlanRequest {

    @NotNull
    @Positive
    private Long destinationId;

    @NotNull
    @Positive
    private Long buildingId;

    @NotNull
    @Positive
    private Long startNodeId;

    @NotNull
    @Positive
    private Long targetNodeId;

    @Pattern(regexp = "^(shortest_distance|shortest_time)$", message = "室内路线策略不合法")
    private String strategyType;

    @Pattern(regexp = "^(elevator|stair|any)$", message = "室内垂直通行方式不合法")
    private String verticalMode;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Long getStartNodeId() {
        return startNodeId;
    }

    public void setStartNodeId(Long startNodeId) {
        this.startNodeId = startNodeId;
    }

    public Long getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(Long targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public String getVerticalMode() {
        return verticalMode;
    }

    public void setVerticalMode(String verticalMode) {
        this.verticalMode = verticalMode;
    }
}
