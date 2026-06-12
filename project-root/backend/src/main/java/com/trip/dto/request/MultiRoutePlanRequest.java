package com.trip.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 多目标路线规划请求。
 */
public class MultiRoutePlanRequest {

    @NotNull
    @Positive
    private Long destinationId;

    @NotNull
    @Positive
    private Long startNodeId;

    @NotEmpty
    @Size(max = 8, message = "目标节点最多支持 8 个")
    private List<@NotNull @Positive Long> targetNodeIds;

    @Pattern(regexp = "^(shortest_distance|shortest_time)$", message = "仅支持 shortest_distance 或 shortest_time")
    private String strategyType;

    @Pattern(regexp = "^(walk|bike|cart|mixed)$", message = "仅支持 walk、bike、cart 或 mixed")
    private String transportType;

    private Boolean returnToStart;

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

    public List<Long> getTargetNodeIds() {
        return targetNodeIds;
    }

    public void setTargetNodeIds(List<Long> targetNodeIds) {
        this.targetNodeIds = targetNodeIds;
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

    public Boolean getReturnToStart() {
        return returnToStart;
    }

    public void setReturnToStart(Boolean returnToStart) {
        this.returnToStart = returnToStart;
    }
}
