package com.trip.vo.response;

import java.util.List;

/**
 * 可用室内导航建筑摘要。
 */
public class IndoorBuildingVO {

    private Long buildingId;
    private Long destinationId;
    private String buildingName;
    private String placeType;
    private String floorInfo;
    private List<Integer> floorNos;

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getPlaceType() {
        return placeType;
    }

    public void setPlaceType(String placeType) {
        this.placeType = placeType;
    }

    public String getFloorInfo() {
        return floorInfo;
    }

    public void setFloorInfo(String floorInfo) {
        this.floorInfo = floorInfo;
    }

    public List<Integer> getFloorNos() {
        return floorNos;
    }

    public void setFloorNos(List<Integer> floorNos) {
        this.floorNos = floorNos;
    }
}
