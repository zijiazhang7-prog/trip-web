package com.trip.vo.response;

import com.trip.dto.map.PathNodeResult;
import java.math.BigDecimal;
import java.util.List;

/**
 * 单目标路线规划结果。
 */
public class RoutePlanVO {

    private Long destinationId;

    private String strategyType;

    private String transportType;

    private BigDecimal totalDistance;

    private Integer estimatedTime;

    private List<PathNodeResult> pathNodes;

    private List<RoutePathEdgeVO> pathEdges;

    private String routeSummary;

    private Long historyId;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
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

    public BigDecimal getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;
    }

    public Integer getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(Integer estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public List<PathNodeResult> getPathNodes() {
        return pathNodes;
    }

    public void setPathNodes(List<PathNodeResult> pathNodes) {
        this.pathNodes = pathNodes;
    }

    public List<RoutePathEdgeVO> getPathEdges() {
        return pathEdges;
    }

    public void setPathEdges(List<RoutePathEdgeVO> pathEdges) {
        this.pathEdges = pathEdges;
    }

    public String getRouteSummary() {
        return routeSummary;
    }

    public void setRouteSummary(String routeSummary) {
        this.routeSummary = routeSummary;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }
}
