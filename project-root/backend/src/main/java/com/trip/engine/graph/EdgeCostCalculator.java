package com.trip.engine.graph;

import com.trip.engine.graph.GraphEngine.GraphEdge;
import com.trip.model.route.RouteTransportType;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class EdgeCostCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;

    public boolean isAccessible(GraphEdge edge, RouteTransportType transportType) {
        return edge != null
                && edge.transportAccess() != null
                && edge.transportAccess().allows(transportType);
    }

    public BigDecimal distanceCost(GraphEdge edge) {
        if (edge == null || edge.distance() == null || edge.distance().compareTo(ZERO) <= 0) {
            return null;
        }
        return edge.distance();
    }

    public BigDecimal timeCost(GraphEdge edge, RouteTransportType transportType) {
        if (!isAccessible(edge, transportType)
                || edge.distance() == null
                || edge.distance().compareTo(ZERO) <= 0
                || edge.crowdFactor() == null
                || edge.crowdFactor().compareTo(ZERO) <= 0
                || edge.crowdFactor().compareTo(ONE) > 0) {
            return null;
        }

        BigDecimal speed = transportType.defaultSpeed();
        if (edge.idealSpeed() != null) {
            if (edge.idealSpeed().compareTo(ZERO) <= 0) {
                return null;
            }
            speed = speed.min(edge.idealSpeed());
        }
        return edge.distance().divide(speed.multiply(edge.crowdFactor()), 8, RoundingMode.HALF_UP);
    }
}
