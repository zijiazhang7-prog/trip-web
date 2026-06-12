package com.trip.taxonomy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaxonomyCatalog {

    private String version;
    private List<String> destTypes = List.of();
    private List<String> interestTags = List.of();
    private List<String> cuisineTags = List.of();
    private Map<String, String> destTypeByCategory = Map.of();
    private Map<String, List<String>> defaultInterestByCategory = Map.of();
    private Map<String, String> interestByTagKeyword = Map.of();
    private Map<String, List<String>> cuisineKeywords = Map.of();
    private Map<String, List<String>> legacyThemeToInterests = Map.of();
    private ScoringWeights scoringWeights = new ScoringWeights();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getDestTypes() {
        return destTypes;
    }

    public void setDestTypes(List<String> destTypes) {
        this.destTypes = destTypes;
    }

    public List<String> getInterestTags() {
        return interestTags;
    }

    public void setInterestTags(List<String> interestTags) {
        this.interestTags = interestTags;
    }

    public List<String> getCuisineTags() {
        return cuisineTags;
    }

    public void setCuisineTags(List<String> cuisineTags) {
        this.cuisineTags = cuisineTags;
    }

    public Map<String, String> getDestTypeByCategory() {
        return destTypeByCategory;
    }

    public void setDestTypeByCategory(Map<String, String> destTypeByCategory) {
        this.destTypeByCategory = destTypeByCategory;
    }

    public Map<String, List<String>> getDefaultInterestByCategory() {
        return defaultInterestByCategory;
    }

    public void setDefaultInterestByCategory(Map<String, List<String>> defaultInterestByCategory) {
        this.defaultInterestByCategory = defaultInterestByCategory;
    }

    public Map<String, String> getInterestByTagKeyword() {
        return interestByTagKeyword;
    }

    public void setInterestByTagKeyword(Map<String, String> interestByTagKeyword) {
        this.interestByTagKeyword = interestByTagKeyword;
    }

    public Map<String, List<String>> getCuisineKeywords() {
        return cuisineKeywords;
    }

    public void setCuisineKeywords(Map<String, List<String>> cuisineKeywords) {
        this.cuisineKeywords = cuisineKeywords;
    }

    public Map<String, List<String>> getLegacyThemeToInterests() {
        return legacyThemeToInterests;
    }

    public void setLegacyThemeToInterests(Map<String, List<String>> legacyThemeToInterests) {
        this.legacyThemeToInterests = legacyThemeToInterests;
    }

    public ScoringWeights getScoringWeights() {
        return scoringWeights;
    }

    public void setScoringWeights(ScoringWeights scoringWeights) {
        this.scoringWeights = scoringWeights;
    }

    public static class ScoringWeights {

        private int destTypeMatch = 3;
        private int interestMatch = 2;
        private int cuisineMatch = 2;

        public int getDestTypeMatch() {
            return destTypeMatch;
        }

        public void setDestTypeMatch(int destTypeMatch) {
            this.destTypeMatch = destTypeMatch;
        }

        public int getInterestMatch() {
            return interestMatch;
        }

        public void setInterestMatch(int interestMatch) {
            this.interestMatch = interestMatch;
        }

        public int getCuisineMatch() {
            return cuisineMatch;
        }

        public void setCuisineMatch(int cuisineMatch) {
            this.cuisineMatch = cuisineMatch;
        }
    }
}
