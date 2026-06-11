package com.trip.engine.index;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class HashIndex {

    private final Map<String, List<Long>> values;

    HashIndex(List<IndexDocument> documents) {
        Map<String, LinkedHashSet<Long>> building = new LinkedHashMap<>();
        for (IndexDocument document : documents) {
            building.computeIfAbsent(document.text(), ignored -> new LinkedHashSet<>()).add(document.id());
        }

        Map<String, List<Long>> snapshot = new LinkedHashMap<>();
        building.forEach((key, ids) -> snapshot.put(key, List.copyOf(ids)));
        values = Map.copyOf(snapshot);
    }

    List<Long> find(String value) {
        return values.getOrDefault(value, List.of());
    }
}
