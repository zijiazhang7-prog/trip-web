package com.trip.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trip.dto.query.DestinationQuery;
import com.trip.dto.query.FacilityQuery;
import com.trip.dto.query.FoodQuery;
import com.trip.dto.query.PlaceQuery;
import com.trip.entity.Destination;
import com.trip.entity.Facility;
import com.trip.entity.Food;
import com.trip.entity.Place;
import java.util.List;

public interface QueryService {

    IPage<Destination> queryDestinations(DestinationQuery query);

    Destination getDestinationById(Long id);

    List<Place> queryPlaces(PlaceQuery query);

    List<Facility> queryFacilities(FacilityQuery query);

    List<Food> queryFoods(FoodQuery query);
}
