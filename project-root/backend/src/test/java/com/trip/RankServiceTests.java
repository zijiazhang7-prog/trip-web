package com.trip;

import com.trip.common.ErrorCode;
import com.trip.entity.Destination;
import com.trip.exception.BusinessException;
import com.trip.service.impl.RankServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankServiceTests {

    private final RankServiceImpl rankService = new RankServiceImpl();

    @Test
    void rankDestinationsShouldSortByHeatDescending() {
        List<Destination> result = rankService.rankDestinations(List.of(
                destination(1L, "A", 30, 4),
                destination(2L, "B", 90, 3),
                destination(3L, "C", 60, 5)), "heat", null);

        assertEquals(List.of(2L, 3L, 1L), ids(result));
    }

    @Test
    void rankDestinationsShouldSortByRatingDescending() {
        List<Destination> result = rankService.rankDestinations(List.of(
                destination(1L, "A", 90, 3),
                destination(2L, "B", 30, 5),
                destination(3L, "C", 60, 4)), "rating", null);

        assertEquals(List.of(2L, 3L, 1L), ids(result));
    }

    @Test
    void rankDestinationsShouldReturnTopK() {
        List<Destination> result = rankService.rankDestinations(List.of(
                destination(1L, "A", 10, 5),
                destination(2L, "B", 40, 4),
                destination(3L, "C", 30, 3),
                destination(4L, "D", 20, 2)), "heat", 2);

        assertEquals(List.of(2L, 3L), ids(result));
    }

    @Test
    void rankDestinationsShouldKeepInputOrderWhenScoreTies() {
        List<Destination> result = rankService.rankDestinations(List.of(
                destination(1L, "A", 50, 4),
                destination(2L, "B", 50, 4),
                destination(3L, "C", 40, 5)), "heat", null);

        assertEquals(List.of(1L, 2L, 3L), ids(result));
    }

    @Test
    void rankDestinationsShouldTreatNullScoreAsZero() {
        Destination noHeat = destination(1L, "A", null, 4);
        Destination withHeat = destination(2L, "B", 1, 4);

        List<Destination> result = rankService.rankDestinations(List.of(noHeat, withHeat), "heat", null);

        assertEquals(List.of(2L, 1L), ids(result));
    }

    @Test
    void rankDestinationsShouldReturnEmptyListForEmptyCandidates() {
        List<Destination> result = rankService.rankDestinations(List.of(), "heat", 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void rankDestinationsShouldRejectInvalidSortBy() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> rankService.rankDestinations(List.of(destination(1L, "A", 1, 1)), "unknown", null));

        assertEquals(ErrorCode.COMMON_008, exception.getErrorCode());
    }

    @Test
    void topKShouldReturnAllWhenKIsLargerThanCandidates() {
        List<Destination> result = rankService.rankDestinations(List.of(
                destination(1L, "A", 10, 4),
                destination(2L, "B", 20, 4)), "heat", 5);

        assertEquals(List.of(2L, 1L), ids(result));
    }

    @Test
    void sortByScoreShouldSupportAscendingOrderForReachableDistance() {
        List<ScoredFacility> result = rankService.sortByScore(List.of(
                new ScoredFacility(1L, new BigDecimal("300.5")),
                new ScoredFacility(2L, new BigDecimal("120.0")),
                new ScoredFacility(3L, new BigDecimal("200.0"))), ScoredFacility::distance, false);

        assertEquals(List.of(2L, 3L, 1L), result.stream().map(ScoredFacility::id).toList());
    }

    @Test
    void topKShouldSupportAscendingOrderForReachableDistance() {
        List<ScoredFacility> result = rankService.topK(List.of(
                new ScoredFacility(1L, new BigDecimal("300.5")),
                new ScoredFacility(2L, new BigDecimal("120.0")),
                new ScoredFacility(3L, new BigDecimal("200.0")),
                new ScoredFacility(4L, new BigDecimal("80.0"))), ScoredFacility::distance, 2, false);

        assertEquals(List.of(4L, 2L), result.stream().map(ScoredFacility::id).toList());
    }

    private Destination destination(Long id, String name, Integer heatScore, Integer ratingScore) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        destination.setHeatScore(heatScore == null ? null : BigDecimal.valueOf(heatScore));
        destination.setRatingScore(ratingScore == null ? null : BigDecimal.valueOf(ratingScore));
        return destination;
    }

    private List<Long> ids(List<Destination> destinations) {
        return destinations.stream().map(Destination::getId).toList();
    }

    private record ScoredFacility(Long id, BigDecimal distance) {
    }
}
