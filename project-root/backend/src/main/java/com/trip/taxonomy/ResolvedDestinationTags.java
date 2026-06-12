package com.trip.taxonomy;

import java.util.List;
import java.util.Set;

public record ResolvedDestinationTags(
        String destType,
        Set<String> interests,
        List<String> rawTags) {

    public static ResolvedDestinationTags empty() {
        return new ResolvedDestinationTags(null, Set.of(), List.of());
    }
}
