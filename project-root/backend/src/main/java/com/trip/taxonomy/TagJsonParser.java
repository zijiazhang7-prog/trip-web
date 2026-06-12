package com.trip.taxonomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.util.StringUtils;

public final class TagJsonParser {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private TagJsonParser() {
    }

    public static List<String> parse(String tagJson, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(tagJson)) {
            return List.of();
        }
        String trimmed = tagJson.trim();
        try {
            List<String> parsed = objectMapper.readValue(trimmed, STRING_LIST_TYPE);
            if (parsed != null) {
                return normalize(parsed);
            }
        } catch (JsonProcessingException ignored) {
            // Historical imports also use pipe-delimited text.
        }
        if (trimmed.contains("|")) {
            return normalize(Arrays.asList(trimmed.split("\\|")));
        }
        return List.of(trimmed);
    }

    private static List<String> normalize(List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                normalized.add(value.trim());
            }
        }
        return List.copyOf(normalized);
    }
}
