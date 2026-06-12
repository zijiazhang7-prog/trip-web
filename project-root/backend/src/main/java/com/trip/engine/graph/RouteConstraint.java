package com.trip.engine.graph;

import com.trip.model.route.RouteTransportType;

public record RouteConstraint(String strategyType, RouteTransportType transportType) {
}
