package com.trip;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.trip.engine.index.IndexDocument;
import com.trip.engine.index.IndexEngine;
import com.trip.engine.index.IndexNamespace;
import com.trip.engine.index.IndexSearchResult;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.Food;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.FoodMapper;
import com.trip.service.impl.IndexMaintenanceServiceImpl;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class IndexMaintenanceServiceTests {

    private final IndexEngine indexEngine = new IndexEngine();
    private final DestinationMapper destinationMapper = mock(DestinationMapper.class);
    private final FoodMapper foodMapper = mock(FoodMapper.class);
    private final DiaryMapper diaryMapper = mock(DiaryMapper.class);
    private final IndexMaintenanceServiceImpl maintenanceService = new IndexMaintenanceServiceImpl(
            indexEngine,
            destinationMapper,
            foodMapper,
            diaryMapper);

    @BeforeAll
    static void initializeTableMetadata() {
        initTableInfo(Destination.class);
        initTableInfo(Food.class);
        initTableInfo(Diary.class);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void rebuildAllShouldBuildAllNamespaces() {
        when(destinationMapper.selectList(any(Wrapper.class))).thenReturn(List.of(destination(1L, "西湖")));
        when(foodMapper.selectList(any(Wrapper.class))).thenReturn(List.of(food(2L, "面条", "一号食堂")));
        when(diaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of(diary(3L, "校园旅行")));

        for (IndexNamespace namespace : IndexNamespace.values()) {
            maintenanceService.rebuild(namespace);
        }

        for (IndexNamespace namespace : IndexNamespace.values()) {
            assertTrue(indexEngine.isReady(namespace));
        }
        assertEquals(List.of(2L), indexEngine.findExact(IndexNamespace.FOOD_SHOP_NAME, "一号食堂").ids());
        assertEquals(List.of(3L), indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "图书馆").ids());
    }

    @Test
    void rebuildAllShouldKeepFailedNamespaceUnavailable() {
        when(destinationMapper.selectList(any(Wrapper.class))).thenThrow(new IllegalStateException("database unavailable"));
        when(foodMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(diaryMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        maintenanceService.rebuildAll();

        assertEquals(false, indexEngine.isReady(IndexNamespace.DESTINATION_NAME));
        assertTrue(indexEngine.isReady(IndexNamespace.FOOD_NAME));
        assertTrue(indexEngine.isReady(IndexNamespace.DIARY_TITLE));
    }

    @Test
    void incrementalUpdateShouldRunOnlyAfterCommit() {
        indexEngine.rebuild(
                IndexNamespace.DIARY_CONTENT,
                List.of(new IndexDocument(1L, "旧正文")));
        beginTransactionSynchronization();

        maintenanceService.upsertAfterCommit(
                IndexNamespace.DIARY_CONTENT,
                new IndexDocument(1L, "新正文"));

        assertEquals(List.of(1L), indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "旧正文").ids());
        assertEquals(
                IndexSearchResult.Status.MISS,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "新正文").status());

        triggerAfterCommit();

        assertEquals(
                IndexSearchResult.Status.MISS,
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "旧正文").status());
        assertEquals(List.of(1L), indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "新正文").ids());
    }

    @Test
    void rollbackShouldLeaveIndexUnchanged() {
        indexEngine.rebuild(
                IndexNamespace.DIARY_CONTENT,
                List.of(new IndexDocument(1L, "事务前正文")));
        beginTransactionSynchronization();

        maintenanceService.removeAfterCommit(IndexNamespace.DIARY_CONTENT, 1L);

        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);

        assertEquals(
                List.of(1L),
                indexEngine.findByContent(IndexNamespace.DIARY_CONTENT, "事务前正文").ids());
    }

    @Test
    void maintenanceFailureShouldInvalidateNamespace() {
        IndexEngine failingEngine = mock(IndexEngine.class);
        IndexMaintenanceServiceImpl failingService = new IndexMaintenanceServiceImpl(
                failingEngine,
                destinationMapper,
                foodMapper,
                diaryMapper);
        doThrow(new IllegalStateException("incremental update failed"))
                .when(failingEngine)
                .upsert(any(IndexNamespace.class), any(IndexDocument.class));

        failingService.upsertAfterCommit(
                IndexNamespace.DIARY_CONTENT,
                new IndexDocument(1L, "正文"));

        verify(failingEngine).invalidate(IndexNamespace.DIARY_CONTENT);
    }

    private Destination destination(Long id, String name) {
        Destination destination = new Destination();
        destination.setId(id);
        destination.setName(name);
        return destination;
    }

    private Food food(Long id, String name, String shopName) {
        Food food = new Food();
        food.setId(id);
        food.setName(name);
        food.setShopName(shopName);
        return food;
    }

    private Diary diary(Long id, String title) {
        Diary diary = new Diary();
        diary.setId(id);
        diary.setTitle(title);
        diary.setContentText("今天参观了图书馆");
        return diary;
    }

    private static void initTableInfo(Class<?> entityType) {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                entityType);
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    private void triggerAfterCommit() {
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization synchronization : synchronizations) {
            synchronization.afterCommit();
        }
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
}
