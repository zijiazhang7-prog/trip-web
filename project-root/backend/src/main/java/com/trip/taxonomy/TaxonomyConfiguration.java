package com.trip.taxonomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class TaxonomyConfiguration {

    @Bean
    public TaxonomyCatalog taxonomyCatalog(ObjectMapper objectMapper) {
        try {
            TaxonomyCatalog catalog = objectMapper.readValue(
                    new ClassPathResource("taxonomy.json").getInputStream(),
                    TaxonomyCatalog.class);
            validate(catalog);
            return catalog;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load taxonomy.json", exception);
        }
    }

    private void validate(TaxonomyCatalog catalog) {
        if (catalog == null
                || catalog.getDestTypes() == null
                || catalog.getDestTypes().isEmpty()
                || catalog.getInterestTags() == null
                || catalog.getInterestTags().isEmpty()
                || catalog.getCuisineTags() == null
                || catalog.getCuisineTags().isEmpty()
                || catalog.getScoringWeights() == null) {
            throw new IllegalStateException("taxonomy.json is incomplete");
        }
    }
}
