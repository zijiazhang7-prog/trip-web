package com.trip.dto.query;

/**
 * 场所候选查询条件。
 */
public class PlaceQuery {

    private Long destinationId;
    private String placeType;

    public Long getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(Long destinationId) {
        this.destinationId = destinationId;
    }

    public String getPlaceType() {
        return placeType;
    }

    public void setPlaceType(String placeType) {
        this.placeType = placeType;
    }
}
