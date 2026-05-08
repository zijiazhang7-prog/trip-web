package com.trip.service;

import com.trip.entity.Destination;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public interface RankService {

    List<Destination> rankDestinations(List<Destination> candidates, String sortBy, Integer topK);

    <T> List<T> sortByScore(List<T> candidates, Function<T, BigDecimal> scoreExtractor, boolean descending);

    <T> List<T> topK(List<T> candidates, Function<T, BigDecimal> scoreExtractor, int k, boolean descending);
}
