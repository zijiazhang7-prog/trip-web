package com.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trip.dto.request.DestinationRecommendQuery;
import com.trip.entity.Destination;
import com.trip.entity.Food;
import com.trip.taxonomy.ResolvedDestinationTags;
import com.trip.taxonomy.TagJsonParser;
import com.trip.taxonomy.TaxonomyConfiguration;
import com.trip.taxonomy.TaxonomyService;
import com.trip.taxonomy.UserTagSelection;
import com.trip.vo.response.UserPreferenceVO;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxonomyServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaxonomyService taxonomyService = new TaxonomyService(
            new TaxonomyConfiguration().taxonomyCatalog(objectMapper),
            objectMapper);

    @Test
    void tagParserShouldSupportJsonPipeAndSingleValue() {
        assertEquals(List.of("历史", "文化"), TagJsonParser.parse("[\"历史\", \"文化\"]", objectMapper));
        assertEquals(List.of("亲子互动", "网红打卡"), TagJsonParser.parse("亲子互动|网红打卡", objectMapper));
        assertEquals(List.of("校园"), TagJsonParser.parse("校园", objectMapper));
    }

    @Test
    void tagParserShouldTrimDeduplicateAndIgnoreEmptyValues() {
        assertEquals(
                List.of("历史", "文化"),
                TagJsonParser.parse(" 历史 | |文化|历史 ", objectMapper));
    }

    @Test
    void destinationShouldResolveCategoryAndPipeTags() {
        Destination destination = new Destination();
        destination.setCategory("历史古迹");
        destination.setTagJson("亲子互动|网红打卡");

        ResolvedDestinationTags resolved = taxonomyService.resolve(destination);

        assertEquals("历史人文", resolved.destType());
        assertTrue(resolved.interests().contains("历史文化"));
        assertTrue(resolved.interests().contains("亲子友好"));
        assertTrue(resolved.interests().contains("网红打卡"));
    }

    @Test
    void legacyPreferenceShouldMapToStandardInterests() {
        UserPreferenceVO preference = UserPreferenceVO.empty(9L);
        preference.setPreferThemeList(List.of("人文建筑型"));
        preference.setPreferFoodType("面食");
        DestinationRecommendQuery query = new DestinationRecommendQuery();
        query.setInterestTags(List.of("网红打卡"));

        UserTagSelection selection = taxonomyService.mergeSelection(query, preference);

        assertTrue(selection.interestTags().contains("历史文化"));
        assertTrue(selection.interestTags().contains("艺术文艺"));
        assertTrue(selection.interestTags().contains("网红打卡"));
        assertTrue(selection.cuisineTags().contains("汤粥面点"));
    }

    @Test
    void foodShouldResolveTagsFromActualTextFields() {
        Food food = new Food();
        food.setName("招牌牛肉面");
        food.setFoodType("地方风味");
        food.setShopName("传统老字号窗口");

        assertTrue(taxonomyService.resolveFood(food).cuisineTags().contains("肉食佳肴"));
        assertTrue(taxonomyService.resolveFood(food).cuisineTags().contains("汤粥面点"));
        assertTrue(taxonomyService.resolveFood(food).cuisineTags().contains("老字号"));
    }
}
