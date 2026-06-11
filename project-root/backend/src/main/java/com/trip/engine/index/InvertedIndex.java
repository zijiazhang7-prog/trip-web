package com.trip.engine.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字符位置倒排索引，支持中文、英文和数字连续子串查询。
 */
final class InvertedIndex {

    private final Map<Integer, Map<Long, List<Integer>>> postings;
    private final Map<Long, List<Integer>> documentCodePoints;

    InvertedIndex(List<IndexDocument> documents) {
        Map<Integer, Map<Long, List<Integer>>> building = new HashMap<>();
        Map<Long, List<Integer>> documentSnapshot = new LinkedHashMap<>();
        for (IndexDocument document : documents) {
            int[] codePoints = document.text().codePoints().toArray();
            documentSnapshot.put(document.id(), toList(codePoints));
            for (int position = 0; position < codePoints.length; position++) {
                building.computeIfAbsent(codePoints[position], ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(document.id(), ignored -> new ArrayList<>())
                        .add(position);
            }
        }

        Map<Integer, Map<Long, List<Integer>>> snapshot = new LinkedHashMap<>();
        building.forEach((codePoint, documentPositions) -> {
            Map<Long, List<Integer>> positionsSnapshot = new LinkedHashMap<>();
            documentPositions.forEach((documentId, positions) ->
                    positionsSnapshot.put(documentId, List.copyOf(positions)));
            snapshot.put(codePoint, immutableMap(positionsSnapshot));
        });
        postings = immutableMap(snapshot);
        documentCodePoints = immutableMap(documentSnapshot);
    }

    private InvertedIndex(
            Map<Integer, Map<Long, List<Integer>>> postings,
            Map<Long, List<Integer>> documentCodePoints) {
        this.postings = immutableMap(postings);
        this.documentCodePoints = immutableMap(documentCodePoints);
    }

    /**
     * 通过写时复制新增或替换单篇文档，只重建受影响字符的 posting 子表。
     */
    InvertedIndex upsert(IndexDocument document) {
        if (document == null || document.id() == null || document.id() <= 0) {
            return this;
        }
        int[] newCodePoints = document.text().codePoints().toArray();
        if (newCodePoints.length == 0) {
            return remove(document.id());
        }

        List<Integer> oldCodePoints = documentCodePoints.get(document.id());
        Map<Integer, List<Integer>> newPositions = positionsByCodePoint(newCodePoints);
        Set<Integer> affectedCodePoints = new LinkedHashSet<>(newPositions.keySet());
        if (oldCodePoints != null) {
            affectedCodePoints.addAll(oldCodePoints);
        }

        Map<Integer, Map<Long, List<Integer>>> updatedPostings = new LinkedHashMap<>(postings);
        for (Integer codePoint : affectedCodePoints) {
            Map<Long, List<Integer>> documentPositions =
                    new LinkedHashMap<>(postings.getOrDefault(codePoint, Map.of()));
            documentPositions.remove(document.id());
            List<Integer> positions = newPositions.get(codePoint);
            if (positions != null) {
                documentPositions.put(document.id(), positions);
            }
            if (documentPositions.isEmpty()) {
                updatedPostings.remove(codePoint);
            } else {
                updatedPostings.put(codePoint, immutableMap(documentPositions));
            }
        }

        Map<Long, List<Integer>> updatedDocuments = new LinkedHashMap<>(documentCodePoints);
        updatedDocuments.put(document.id(), toList(newCodePoints));
        return new InvertedIndex(updatedPostings, updatedDocuments);
    }

    /**
     * 通过写时复制删除单篇文档及其 posting 数据。
     */
    InvertedIndex remove(Long documentId) {
        if (documentId == null || documentId <= 0) {
            return this;
        }
        List<Integer> oldCodePoints = documentCodePoints.get(documentId);
        if (oldCodePoints == null) {
            return this;
        }

        Map<Integer, Map<Long, List<Integer>>> updatedPostings = new LinkedHashMap<>(postings);
        for (Integer codePoint : new LinkedHashSet<>(oldCodePoints)) {
            Map<Long, List<Integer>> documentPositions =
                    new LinkedHashMap<>(postings.getOrDefault(codePoint, Map.of()));
            documentPositions.remove(documentId);
            if (documentPositions.isEmpty()) {
                updatedPostings.remove(codePoint);
            } else {
                updatedPostings.put(codePoint, immutableMap(documentPositions));
            }
        }

        Map<Long, List<Integer>> updatedDocuments = new LinkedHashMap<>(documentCodePoints);
        updatedDocuments.remove(documentId);
        return new InvertedIndex(updatedPostings, updatedDocuments);
    }

    List<Long> findContaining(String keyword) {
        int[] keywordCodePoints = keyword.codePoints().toArray();
        if (keywordCodePoints.length == 0) {
            return List.of();
        }

        Map<Long, List<Integer>> firstPostings = postings.get(keywordCodePoints[0]);
        if (firstPostings == null || firstPostings.isEmpty()) {
            return List.of();
        }

        Set<Long> matchedIds = new LinkedHashSet<>();
        for (Map.Entry<Long, List<Integer>> entry : firstPostings.entrySet()) {
            if (containsConsecutiveKeyword(entry.getKey(), entry.getValue(), keywordCodePoints)) {
                matchedIds.add(entry.getKey());
            }
        }
        return List.copyOf(matchedIds);
    }

    private boolean containsConsecutiveKeyword(
            Long documentId,
            List<Integer> startPositions,
            int[] keywordCodePoints) {
        if (keywordCodePoints.length == 1) {
            return true;
        }

        for (Integer startPosition : startPositions) {
            boolean matched = true;
            for (int offset = 1; offset < keywordCodePoints.length; offset++) {
                Map<Long, List<Integer>> codePointPostings = postings.get(keywordCodePoints[offset]);
                List<Integer> positions = codePointPostings == null ? null : codePointPostings.get(documentId);
                if (positions == null || !containsPosition(positions, startPosition + offset)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPosition(List<Integer> positions, int expectedPosition) {
        int low = 0;
        int high = positions.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            int value = positions.get(middle);
            if (value == expectedPosition) {
                return true;
            }
            if (value < expectedPosition) {
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return false;
    }

    private Map<Integer, List<Integer>> positionsByCodePoint(int[] codePoints) {
        Map<Integer, List<Integer>> positions = new LinkedHashMap<>();
        for (int position = 0; position < codePoints.length; position++) {
            positions.computeIfAbsent(codePoints[position], ignored -> new ArrayList<>()).add(position);
        }
        positions.replaceAll((ignored, values) -> List.copyOf(values));
        return positions;
    }

    private static List<Integer> toList(int[] codePoints) {
        List<Integer> values = new ArrayList<>(codePoints.length);
        for (int codePoint : codePoints) {
            values.add(codePoint);
        }
        return List.copyOf(values);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
