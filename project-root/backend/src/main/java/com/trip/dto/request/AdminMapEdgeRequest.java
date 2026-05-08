package com.trip.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Admin 地图边维护请求。
 */
public class AdminMapEdgeRequest {

    @NotNull
    @Min(1)
    private Long destinationId;

    @NotNull
    @Min(1)
    private Long fromNodeId;

    @NotNull
    @Min(1)
    private Long toNodeId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal distance;

    @DecimalMin(value = "0.01")
    private BigDecimal idealSpeed;

    @DecimalMin(value = "0.01")
    private BigDecimal crowdFactor;

    @Size(max = 20)
    private String transportType;

    @Size(max = 20)
    private String edgeType;

    @Min(0)
    private Integer bidirectionalFlag;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public Long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(Long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public Long getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(Long toNodeId) {
        this.toNodeId = toNodeId;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public BigDecimal getIdealSpeed() {
        return idealSpeed;
    }

    public void setIdealSpeed(BigDecimal idealSpeed) {
        this.idealSpeed = idealSpeed;
    }

    public BigDecimal getCrowdFactor() {
        return crowdFactor;
    }

    public void setCrowdFactor(BigDecimal crowdFactor) {
        this.crowdFactor = crowdFactor;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getEdgeType() {
        return edgeType;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public Integer getBidirectionalFlag() {
        return bidirectionalFlag;
    }

    public void setBidirectionalFlag(Integer bidirectionalFlag) {
        this.bidirectionalFlag = bidirectionalFlag;
    }
}
