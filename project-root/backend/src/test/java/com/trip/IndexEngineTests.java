package com.trip;

import com.trip.engine.index.IndexDocument;
import com.trip.engine.index.IndexEngine;
import com.trip.engine.index.IndexNamespace;
import com.trip.engine.index.IndexSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexEngineTests {

    private final IndexEngine indexEngine = new IndexEngine();

    @Test
    void exactSearchShouldNormalizeTextAndReturnDuplicateNameIds() {
        indexEngine.rebuild(IndexNamespace.DESTINATION_NAME, List.of(
                new IndexDocument(1L, " 西湖 "),
                new IndexDocument(2L, "西湖"),
                new IndexDocument(3L, "灵隐寺")));

        IndexSearchResult result = indexEngine.findExact(IndexNamespace.DESTINATION_NAME, "西湖");

        assertEquals(IndexSearchResult.Status.HIT, result.status());
        assertEquals(List.of(1L, 2L), result.ids());
    }

    @Test
    void prefixSearchShouldSupportChineseAndLimitResults() {
        indexEngine.rebuild(IndexNamespace.DIARY_TITLE, List.of(
                new IndexDocument(1L, "校园旅行第一天"),
                new IndexDocument(2L, "校园旅行第二天"),
                new IndexDocument(3L, "城市漫步")));

        IndexSearchResult result = indexEngine.findByPrefix(IndexNamespace.DIARY_TITLE, "校园", 1);

        assertEquals(IndexSearchResult.Status.HIT, result.status());
        assertEquals(List.of(1L), result.ids());
    }

    @Test
    void rebuildShouldReplaceOldSnapshotAndIgnoreInvalidDocuments() {
        indexEngine.rebuild(IndexNamespace.FOOD_NAME, List.of(new IndexDocument(1L, "Noodles")));
        indexEngine.rebuild(IndexNamespace.FOOD_NAME, List.of(
                new IndexDocument(2L, "Tea"),
                new IndexDocument(null, "Invalid"),
                new IndexDocument(3L, " ")));

        assertEquals(IndexSearchResult.Status.MISS,
                indexEngine.findExact(IndexNamespace.FOOD_NAME, "noodles").status());
        assertEquals(List.of(2L), indexEngine.findExact(IndexNamespace.FOOD_NAME, "TEA").ids());
    }

    @Test
    void invalidateShouldMakeOnlySelectedNamespaceUnavailable() {
        indexEngine.rebuild(IndexNamespace.FOOD_NAME, List.of(new IndexDocument(1L, "面条")));
        indexEngine.rebuild(IndexNamespace.FOOD_SHOP_NAME, List.of(new IndexDocument(1L, "一号食堂")));

        indexEngine.invalidate(IndexNamespace.FOOD_NAME);

        assertFalse(indexEngine.isReady(IndexNamespace.FOOD_NAME));
        assertEquals(IndexSearchResult.Status.UNAVAILABLE,
                indexEngine.findExact(IndexNamespace.FOOD_NAME, "面条").status());
        assertTrue(indexEngine.isReady(IndexNamespace.FOOD_SHOP_NAME));
    }

    @Test
    void invertedIndexShouldMatchChineseContinuousSubstring() {
        indexEngine.rebuild(IndexNamespace.DIARY_CONTENT, List.of(
                new IndexDocument(1L, "今天参观了图书馆和教学楼"),
                new IndexDocument(2L, "今天只参观了教学楼")));

        IndexSearchResult result =
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "图书馆");

        assertEquals(IndexSearchResult.Status.HIT, result.status());
        assertEquals(List.of(1L), result.ids());
    }

    @Test
    void invertedIndexShouldNormalizeCaseAndSupportSingleCharacter() {
        indexEngine.rebuild(IndexNamespace.DIARY_CONTENT, List.of(
                new IndexDocument(1L, "Campus Walk"),
                new IndexDocument(2L, "城市漫步")));

        assertEquals(
                List.of(1L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "CAMPUS").ids());
        assertEquals(
                List.of(2L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "漫").ids());
    }

    @Test
    void invertedIndexShouldRejectWrongOrderOrNonConsecutiveCharacters() {
        indexEngine.rebuild(IndexNamespace.DIARY_CONTENT, List.of(
                new IndexDocument(1L, "图书馆"),
                new IndexDocument(2L, "图书和展览馆"),
                new IndexDocument(3L, "馆书图")));

        assertEquals(
                List.of(1L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "图书馆").ids());
        assertEquals(
                IndexSearchResult.Status.MISS,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "书图馆").status());
    }

    @Test
    void invertedIndexRebuildAndInvalidateShouldReplaceSnapshot() {
        indexEngine.rebuild(
                IndexNamespace.DIARY_CONTENT,
                List.of(new IndexDocument(1L, "旧正文")));
        indexEngine.rebuild(
                IndexNamespace.DIARY_CONTENT,
                List.of(new IndexDocument(2L, "新正文")));

        assertEquals(
                IndexSearchResult.Status.MISS,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "旧正文").status());
        assertEquals(
                List.of(2L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "新正文").ids());

        indexEngine.invalidate(IndexNamespace.DIARY_CONTENT);

        assertEquals(
                IndexSearchResult.Status.UNAVAILABLE,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "新正文").status());
    }

    @Test
    void invertedIndexShouldIncrementallyAddReplaceAndRemoveDocument() {
        indexEngine.rebuild(
                IndexNamespace.DIARY_CONTENT,
                List.of(new IndexDocument(1L, "保留正文")));

        indexEngine.upsert(
                IndexNamespace.DIARY_CONTENT,
                new IndexDocument(2L, "新增 Campus 😀 正文"));

        assertEquals(
                List.of(2L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "CAMPUS 😀").ids());

        indexEngine.upsert(
                IndexNamespace.DIARY_CONTENT,
                new IndexDocument(2L, "替换后的图书馆正文"));

        assertEquals(
                IndexSearchResult.Status.MISS,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "campus").status());
        assertEquals(
                List.of(2L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "图书馆").ids());

        indexEngine.remove(IndexNamespace.DIARY_CONTENT, 2L);

        assertEquals(
                IndexSearchResult.Status.MISS,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "图书馆").status());
        assertEquals(
                List.of(1L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "保留").ids());
    }

    @Test
    void incrementalMutationShouldNotCreatePartialUnavailableIndex() {
        indexEngine.upsert(
                IndexNamespace.DIARY_CONTENT,
                new IndexDocument(1L, "不能创建不完整索引"));
        indexEngine.remove(IndexNamespace.DIARY_CONTENT, 1L);

        assertEquals(
                IndexSearchResult.Status.UNAVAILABLE,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "不完整").status());
    }

    @Test
    void emptyIncrementalContentShouldRemoveExistingDocument() {
        indexEngine.rebuild(
                IndexNamespace.DIARY_CONTENT,
                List.of(new IndexDocument(1L, "原正文")));

        indexEngine.upsert(IndexNamespace.DIARY_CONTENT, new IndexDocument(1L, " "));

        assertEquals(
                IndexSearchResult.Status.MISS,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "原正文").status());
    }
}
