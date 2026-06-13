package com.trip.vo.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 室内单目标路线规划响应。
 */
public class IndoorRoutePlanVO {

    private Long destinationId;
    private Long buildingId;
    private String buildingName;
    private String strategyType;
    private String verticalMode;
    private BigDecimal totalDistance;
    private Integer totalTime;
    private String timeUnit;
    private List<IndoorNodeVO> pathNodes;
    private List<IndoorEdgeVO> pathEdges;
    private List<String> steps;

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

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
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

    public BigDecimal getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;
    }

    public Integer getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Integer totalTime) {
        this.totalTime = totalTime;
    }

    public String getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(String timeUnit) {
        this.timeUnit = timeUnit;
    }

    public List<IndoorNodeVO> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<IndoorNodeVO> pathNodes) {
        this.pathNodes = pathNodes;
    }

    public List<IndoorEdgeVO> getPathEdges() {
        return pathEdges;
    }

    public void setPathEdges(List<IndoorEdgeVO> pathEdges) {
        this.pathEdges = pathEdges;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }
}
