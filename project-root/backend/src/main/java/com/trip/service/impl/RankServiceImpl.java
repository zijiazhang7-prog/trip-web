package com.trip.service.impl;

import com.trip.common.ErrorCode;
import com.trip.entity.Destination;
import com.trip.exception.BusinessException;
import com.trip.service.RankService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RankServiceImpl implements RankService {

    private static final String SORT_BY_HEAT = "heat";
    private static final String SORT_BY_RATING = "rating";

    @Override
    public List<Destination> rankDestinations(List<Destination> candidates, String sortBy, Integer topK) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        Function<Destination, BigDecimal> scoreExtractor = switch (normalizedSortBy) {
            case SORT_BY_HEAT -> Destination::getHeatScore;
            case SORT_BY_RATING -> Destination::getRatingScore;
            default -> throw new BusinessException(ErrorCode.COMMON_008);
        };

        if (topK == null || topK <= 0) {
            return sortByScore(candidates, scoreExtractor, true);
        }
        return topK(candidates, scoreExtractor, topK, true);
    }

    @Override
    public <T> List<T> sortByScore(List<T> candidates, Function<T, BigDecimal> scoreExtractor, boolean descending) {
        List<IndexedScoreItem<T>> items = wrap(candidates, scoreExtractor);
        items.sort(bestFirstComparator(descending));
        return unwrap(items);
    }

    @Override
    public <T> List<T> topK(List<T> candidates, Function<T, BigDecimal> scoreExtractor, int k, boolean descending) {
        if (candidates == null || candidates.isEmpty() || k <= 0) {
            return List.of();
        }

        PriorityQueue<IndexedScoreItem<T>> heap = new PriorityQueue<>(worstFirstComparator(descending));
        Comparator<IndexedScoreItem<T>> bestComparator = bestFirstComparator(descending);
        for (int i = 0; i < candidates.size(); i++) {
            T value = candidates.get(i);
            IndexedScoreItem<T> item = new IndexedScoreItem<>(value, scoreOf(value, scoreExtractor), i);
            if (heap.size() < k) {
                heap.offer(item);
            } else if (bestComparator.compare(item, heap.peek()) < 0) {
                heap.poll();
                heap.offer(item);
            }
        }

        List<IndexedScoreItem<T>> items = new ArrayList<>(heap);
        items.sort(bestComparator);
        return unwrap(items);
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) {
            return SORT_BY_HEAT;
        }
        return sortBy.trim().toLowerCase();
    }

    private <T> List<IndexedScoreItem<T>> wrap(List<T> candidates, Function<T, BigDecimal> scoreExtractor) {
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        List<IndexedScoreItem<T>> items = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            T value = candidates.get(i);
            items.add(new IndexedScoreItem<>(value, scoreOf(value, scoreExtractor), i));
        }
        return items;
    }

    private <T> BigDecimal scoreOf(T value, Function<T, BigDecimal> scoreExtractor) {
        if (value == null || scoreExtractor == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal score = scoreExtractor.apply(value);
        return score == null ? BigDecimal.ZERO : score;
    }

    private <T> Comparator<IndexedScoreItem<T>> bestFirstComparator(boolean descending) {
        return (left, right) -> {
            int scoreCompare = descending
                    ? right.score().compareTo(left.score())
                    : left.score().compareTo(right.score());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Integer.compare(left.index(), right.index());
        };
    }

    private <T> Comparator<IndexedScoreItem<T>> worstFirstComparator(boolean descending) {
        return (left, right) -> {
            int scoreCompare = descending
                    ? left.score().compareTo(right.score())
                    : right.score().compareTo(left.score());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            return Integer.compare(right.index(), left.index());
        };
    }

    private <T> List<T> unwrap(List<IndexedScoreItem<T>> items) {
        return items.stream().map(IndexedScoreItem::value).toList();
    }

    private record IndexedScoreItem<T>(T value, BigDecimal score, int index) {
    }
}
