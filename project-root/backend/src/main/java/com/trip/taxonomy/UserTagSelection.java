package com.trip.taxonomy;

import java.util.List;

public record UserTagSelection(
        List<String> destTypes,
        List<String> interestTags,
        List<String> cuisineTags) {

    public UserTagSelection {
        destTypes = destTypes == null ? List.of() : List.copyOf(destTypes);
        interestTags = interestTags == null ? List.of() : List.copyOf(interestTags);
        cuisineTags = cuisineTags == null ? List.of() : List.copyOf(cuisineTags);
    }

    public static UserTagSelection empty() {
        return new UserTagSelection(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return destTypes.isEmpty() && interestTags.isEmpty() && cuisineTags.isEmpty();
    }
}
