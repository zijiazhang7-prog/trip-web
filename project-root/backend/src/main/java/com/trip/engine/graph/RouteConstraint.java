package com.trip.engine.graph;

import com.trip.model.route.RouteTransportType;
import java.util.Set;

public record RouteConstraint(
        String strategyType,
        RouteTransportType transportType,
        Set<String> allowedEdgeTypes) {

    public RouteConstraint(String strategyType, RouteTransportType transportType) {
        this(strategyType, transportType, Set.of());
    }

    public RouteConstraint {
        allowedEdgeTypes = allowedEdgeTypes == null ? Set.of() : Set.copyOf(allowedEdgeTypes);
    }

    public boolean allowsEdgeType(String edgeType) {
        return allowedEdgeTypes.isEmpty() || allowedEdgeTypes.contains(edgeType);
    }
}
