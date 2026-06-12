package com.trip.taxonomy;

import java.util.Set;

public record ResolvedFoodTags(Set<String> cuisineTags) {

    public static ResolvedFoodTags empty() {
        return new ResolvedFoodTags(Set.of());
    }
}
