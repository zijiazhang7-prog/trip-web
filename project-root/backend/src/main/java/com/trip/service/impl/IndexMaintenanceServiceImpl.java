package com.trip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trip.engine.index.IndexDocument;
import com.trip.engine.index.IndexEngine;
import com.trip.engine.index.IndexNamespace;
import com.trip.entity.Destination;
import com.trip.entity.Diary;
import com.trip.entity.Food;
import com.trip.mapper.DestinationMapper;
import com.trip.mapper.DiaryMapper;
import com.trip.mapper.FoodMapper;
import com.trip.service.IndexMaintenanceService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 索引维护服务：应用启动预热、手动重建和失效。
 */
@Service
public class IndexMaintenanceServiceImpl implements IndexMaintenanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexMaintenanceServiceImpl.class);
    private static final int ENABLED_STATUS = 1;
    private static final String VISIBILITY_PUBLIC = "public";

    private final IndexEngine indexEngine;
    private final DestinationMapper destinationMapper;
    private final FoodMapper foodMapper;
    private final DiaryMapper diaryMapper;

    public IndexMaintenanceServiceImpl(
            IndexEngine indexEngine,
            DestinationMapper destinationMapper,
            FoodMapper foodMapper,
            DiaryMapper diaryMapper) {
        this.indexEngine = indexEngine;
        this.destinationMapper = destinationMapper;
        this.foodMapper = foodMapper;
        this.diaryMapper = diaryMapper;
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildAll() {
        for (IndexNamespace namespace : IndexNamespace.values()) {
            try {
                rebuild(namespace);
            } catch (RuntimeException exception) {
                indexEngine.invalidate(namespace);
                LOGGER.warn("Index warm-up failed for namespace {}", namespace);
            }
        }
    }

    @Override
    public void rebuild(IndexNamespace namespace) {
        if (namespace == null) {
            return;
        }
        List<IndexDocument> documents = switch (namespace) {
            case DESTINATION_NAME -> destinationDocuments();
            case FOOD_NAME -> foodNameDocuments();
            case FOOD_SHOP_NAME -> foodShopDocuments();
            case DIARY_TITLE -> diaryTitleDocuments();
            case DIARY_CONTENT -> diaryContentDocuments();
        };
        indexEngine.rebuild(namespace, documents);
    }

    @Override
    public void invalidate(IndexNamespace namespace) {
        indexEngine.invalidate(namespace);
    }

    @Override
    public void upsertAfterCommit(IndexNamespace namespace, IndexDocument document) {
        executeAfterCommit(namespace, () -> indexEngine.upsert(namespace, document));
    }

    @Override
    public void removeAfterCommit(IndexNamespace namespace, Long documentId) {
        executeAfterCommit(namespace, () -> indexEngine.remove(namespace, documentId));
    }

    @Override
    public void invalidateAfterCommit(IndexNamespace namespace) {
        executeAfterCommit(namespace, () -> indexEngine.invalidate(namespace));
    }

    private void executeAfterCommit(IndexNamespace namespace, Runnable action) {
        if (namespace == null || action == null) {
            return;
        }
        Runnable safeAction = () -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                indexEngine.invalidate(namespace);
                LOGGER.warn("Incremental index maintenance failed for namespace {}", namespace);
            }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeAction.run();
                }
            });
            return;
        }
        safeAction.run();
    }

    private List<IndexDocument> destinationDocuments() {
        return destinationMapper.selectList(new LambdaQueryWrapper<Destination>()
                        .select(Destination::getId, Destination::getName)
                        .eq(Destination::getStatus, ENABLED_STATUS))
                .stream()
                .map(destination -> new IndexDocument(destination.getId(), destination.getName()))
                .toList();
    }

    private List<IndexDocument> foodNameDocuments() {
        return foodMapper.selectList(new LambdaQueryWrapper<Food>()
                        .select(Food::getId, Food::getName))
                .stream()
                .map(food -> new IndexDocument(food.getId(), food.getName()))
                .toList();
    }

    private List<IndexDocument> foodShopDocuments() {
        return foodMapper.selectList(new LambdaQueryWrapper<Food>()
                        .select(Food::getId, Food::getShopName))
                .stream()
                .map(food -> new IndexDocument(food.getId(), food.getShopName()))
                .toList();
    }

    private List<IndexDocument> diaryTitleDocuments() {
        return diaryMapper.selectList(new LambdaQueryWrapper<Diary>()
                        .select(Diary::getId, Diary::getTitle)
                        .eq(Diary::getStatus, ENABLED_STATUS)
                        .eq(Diary::getVisibility, VISIBILITY_PUBLIC))
                .stream()
                .map(diary -> new IndexDocument(diary.getId(), diary.getTitle()))
                .toList();
    }

    private List<IndexDocument> diaryContentDocuments() {
        return diaryMapper.selectList(new LambdaQueryWrapper<Diary>()
                        .select(Diary::getId, Diary::getContentText)
                        .eq(Diary::getStatus, ENABLED_STATUS)
                        .eq(Diary::getVisibility, VISIBILITY_PUBLIC))
                .stream()
                .map(diary -> new IndexDocument(diary.getId(), diary.getContentText()))
                .toList();
    }
}
