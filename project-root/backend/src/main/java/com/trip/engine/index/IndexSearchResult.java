package com.trip.engine.index;

import java.util.List;

/**
 * 索引查询结果，明确区分不可用、已命中和已构建但未命中。
 */
public record IndexSearchResult(Status status, List<Long> ids) {

    public enum Status {
        UNAVAILABLE,
        HIT,
        MISS
    }

    public static IndexSearchResult unavailable() {
        return new IndexSearchResult(Status.UNAVAILABLE, List.of());
    }

    public static IndexSearchResult available(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : List.copyOf(ids);
        return new IndexSearchResult(safeIds.isEmpty() ? Status.MISS : Status.HIT, safeIds);
    }

    public boolean isAvailable() {
        return status != Status.UNAVAILABLE;
    }
}
