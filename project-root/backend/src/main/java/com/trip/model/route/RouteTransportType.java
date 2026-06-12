package com.trip.model.route;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

public enum RouteTransportType {

    WALK("walk", new BigDecimal("80")),
    BIKE("bike", new BigDecimal("250")),
    CART("cart", new BigDecimal("300")),
    MIXED("mixed", null);

    private final String value;
    private final BigDecimal defaultSpeed;

    RouteTransportType(String value, BigDecimal defaultSpeed) {
        this.value = value;
        this.defaultSpeed = defaultSpeed;
    }

    public String value() {
        return value;
    }

    public BigDecimal defaultSpeed() {
        return defaultSpeed;
    }

    public boolean isSingleTransport() {
        return this != MIXED;
    }

    public static Optional<RouteTransportType> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (RouteTransportType type : values()) {
            if (type.value.equals(normalized)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
