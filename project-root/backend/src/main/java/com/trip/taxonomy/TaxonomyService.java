package com.trip.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.request.DestinationRecommendQuery;
import com.trip.entity.Destination;
import com.trip.entity.Food;
import com.trip.vo.response.UserPreferenceVO;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TaxonomyService {

    private final TaxonomyCatalog catalog;
    private final ObjectMapper objectMapper;

    public TaxonomyService(TaxonomyCatalog catalog, ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    public ResolvedDestinationTags resolve(Destination destination) {
        if (destination == null) {
            return ResolvedDestinationTags.empty();
        }
        String category = normalize(destination.getCategory());
        String destType = catalog.getDestTypeByCategory().get(category);
        LinkedHashSet<String> interests = new LinkedHashSet<>(
                catalog.getDefaultInterestByCategory().getOrDefault(category, List.of()));
        List<String> rawTags = TagJsonParser.parse(destination.getTagJson(), objectMapper);
        for (String rawTag : rawTags) {
            String normalizedTag = rawTag.toLowerCase(Locale.ROOT);
            addAll(interests, catalog.getLegacyThemeToInterests().get(rawTag));
            catalog.getInterestByTagKeyword().forEach((keyword, interest) -> {
                if (normalizedTag.contains(keyword.toLowerCase(Locale.ROOT))) {
                    interests.add(interest);
                }
            });
        }
        return new ResolvedDestinationTags(
                destType,
                Collections.unmodifiableSet(new LinkedHashSet<>(interests)),
                rawTags);
    }

    public ResolvedFoodTags resolveFood(Food food) {
        if (food == null) {
            return ResolvedFoodTags.empty();
        }
        String searchable = String.join(" ",
                normalizeToEmpty(food.getName()),
                normalizeToEmpty(food.getFoodType()),
                normalizeToEmpty(food.getShopName())).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> cuisines = new LinkedHashSet<>();
        catalog.getCuisineKeywords().forEach((cuisine, keywords) -> {
            if (keywords.stream()
                    .filter(StringUtils::hasText)
                    .map(value -> value.toLowerCase(Locale.ROOT))
                    .anyMatch(searchable::contains)) {
                cuisines.add(cuisine);
            }
        });
        return new ResolvedFoodTags(Collections.unmodifiableSet(new LinkedHashSet<>(cuisines)));
    }

    public int scoreDestination(ResolvedDestinationTags tags, UserTagSelection selection) {
        if (tags == null || selection == null || selection.isEmpty()) {
            return 0;
        }
        int score = 0;
        if (tags.destType() != null && selection.destTypes().contains(tags.destType())) {
            score += catalog.getScoringWeights().getDestTypeMatch();
        }
        for (String interest : tags.interests()) {
            if (selection.interestTags().contains(interest)) {
                score += catalog.getScoringWeights().getInterestMatch();
            }
        }
        return score;
    }

    public int scoreFood(ResolvedFoodTags tags, UserTagSelection selection) {
        if (tags == null || selection == null || selection.cuisineTags().isEmpty()) {
            return 0;
        }
        long matches = tags.cuisineTags().stream().filter(selection.cuisineTags()::contains).count();
        return Math.toIntExact(matches * catalog.getScoringWeights().getCuisineMatch());
    }

    public UserTagSelection mergeSelection(
            DestinationRecommendQuery query,
            UserPreferenceVO preference) {
        LinkedHashSet<String> destTypes = new LinkedHashSet<>();
        LinkedHashSet<String> interests = new LinkedHashSet<>();
        LinkedHashSet<String> cuisines = new LinkedHashSet<>();
        if (query != null) {
            add(destTypes, query.getDestType());
            addAll(interests, query.getInterestTags());
        }
        mergePreference(preference, interests, cuisines);
        return new UserTagSelection(List.copyOf(destTypes), List.copyOf(interests), List.copyOf(cuisines));
    }

    public UserTagSelection foodSelection(List<String> cuisineTags, UserPreferenceVO preference) {
        LinkedHashSet<String> interests = new LinkedHashSet<>();
        LinkedHashSet<String> cuisines = new LinkedHashSet<>();
        addAll(cuisines, cuisineTags);
        mergePreference(preference, interests, cuisines);
        return new UserTagSelection(List.of(), List.copyOf(interests), List.copyOf(cuisines));
    }

    private void mergePreference(
            UserPreferenceVO preference,
            Set<String> interests,
            Set<String> cuisines) {
        if (preference == null) {
            return;
        }
        if (preference.getPreferThemeList() != null) {
            for (String theme : preference.getPreferThemeList()) {
                String normalized = normalize(theme);
                if (!StringUtils.hasText(normalized)) {
                    continue;
                }
                List<String> mapped = catalog.getLegacyThemeToInterests().get(normalized);
                if (mapped == null || mapped.isEmpty()) {
                    if (catalog.getInterestTags().contains(normalized)) {
                        interests.add(normalized);
                    } else {
                        String lowerTheme = normalized.toLowerCase(Locale.ROOT);
                        catalog.getInterestByTagKeyword().forEach((keyword, interest) -> {
                            if (lowerTheme.contains(keyword.toLowerCase(Locale.ROOT))) {
                                interests.add(interest);
                            }
                        });
                    }
                } else {
                    addAll(interests, mapped);
                }
            }
        }
        addCuisinePreference(cuisines, preference.getPreferFoodType());
    }

    private void addCuisinePreference(Set<String> cuisines, String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return;
        }
        if (catalog.getCuisineTags().contains(normalized)) {
            cuisines.add(normalized);
            return;
        }
        String lowerValue = normalized.toLowerCase(Locale.ROOT);
        catalog.getCuisineKeywords().forEach((cuisine, keywords) -> {
            if (keywords.stream()
                    .filter(StringUtils::hasText)
                    .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                    .anyMatch(lowerValue::contains)) {
                cuisines.add(cuisine);
            }
        });
    }

    private void addAll(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            add(target, value);
        }
    }

    private void add(Set<String> target, String value) {
        String normalized = normalize(value);
        if (StringUtils.hasText(normalized)) {
            target.add(normalized);
        }
    }

    private String normalizeToEmpty(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
