package com.trip.model.route;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum EdgeTransportAccess {

    WALK("walk", RouteTransportType.WALK),
    BIKE("bike", RouteTransportType.BIKE),
    CART("cart", RouteTransportType.CART),
    WALK_BIKE("walk_bike", RouteTransportType.WALK, RouteTransportType.BIKE),
    WALK_CART("walk_cart", RouteTransportType.WALK, RouteTransportType.CART),
    BIKE_CART("bike_cart", RouteTransportType.BIKE, RouteTransportType.CART),
    ALL("all", RouteTransportType.WALK, RouteTransportType.BIKE, RouteTransportType.CART);

    private final String value;
    private final Set<RouteTransportType> allowedTypes;

    EdgeTransportAccess(String value, RouteTransportType first, RouteTransportType... others) {
        this.value = value;
        this.allowedTypes = EnumSet.of(first, others);
    }

    public String value() {
        return value;
    }

    public boolean allows(RouteTransportType transportType) {
        return transportType != null && transportType.isSingleTransport() && allowedTypes.contains(transportType);
    }

    public Set<RouteTransportType> allowedTypes() {
        return Set.copyOf(allowedTypes);
    }

    public static Optional<EdgeTransportAccess> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (EdgeTransportAccess access : values()) {
            if (access.value.equals(normalized)) {
                return Optional.of(access);
            }
        }
        return Optional.empty();
    }
}
