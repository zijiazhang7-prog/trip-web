package com.trip.model.route;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 室内路线允许使用的垂直通行方式。
 */
public enum IndoorVerticalMode {

    ELEVATOR("elevator", Set.of("corridor", "elevator")),
    STAIR("stair", Set.of("corridor", "stair")),
    ANY("any", Set.of("corridor", "elevator", "stair"));

    private final String value;
    private final Set<String> allowedEdgeTypes;

    IndoorVerticalMode(String value, Set<String> allowedEdgeTypes) {
        this.value = value;
        this.allowedEdgeTypes = Set.copyOf(allowedEdgeTypes);
    }

    public String value() {
        return value;
    }

    public Set<String> allowedEdgeTypes() {
        return allowedEdgeTypes;
    }

    public static Optional<IndoorVerticalMode> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (IndoorVerticalMode mode : values()) {
            if (mode.value.equals(normalized)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
