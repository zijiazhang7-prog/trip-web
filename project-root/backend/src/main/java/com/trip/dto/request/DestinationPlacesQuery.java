package com.trip.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 目的地下场所列表查询参数。
 */
public class DestinationPlacesQuery {

    @Size(max = 50)
    private String placeType;

    public String getPlaceType() {
        return placeType;
    }

    public void setPlaceType(String placeType) {
        this.placeType = placeType;
    }
}
