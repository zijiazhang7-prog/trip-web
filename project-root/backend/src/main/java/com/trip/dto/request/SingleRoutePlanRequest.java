package com.trip.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 单目标路线规划请求。
 */
public class SingleRoutePlanRequest {

    @NotNull
    @Positive
    private Long destinationId;

    @NotNull
    @Positive
    private Long startNodeId;

    @NotNull
    @Positive
    private Long targetNodeId;

    @Pattern(regexp = "^(shortest_distance|shortest_time)$", message = "仅支持 shortest_distance 或 shortest_time")
    private String strategyType;

    @Pattern(regexp = "^(walk|bike|cart|mixed)$", message = "仅支持 walk、bike、cart 或 mixed")
    private String transportType;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
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

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }
}
