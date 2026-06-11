package com.trip.service;

import com.trip.engine.index.IndexDocument;
import com.trip.engine.index.IndexNamespace;

/**
 * 负责从数据库加载索引源数据，并管理索引重建和失效。
 */
public interface IndexMaintenanceService {

    void rebuildAll();

    void rebuild(IndexNamespace namespace);

    void invalidate(IndexNamespace namespace);

    void upsertAfterCommit(IndexNamespace namespace, IndexDocument document);

    void removeAfterCommit(IndexNamespace namespace, Long documentId);

    void invalidateAfterCommit(IndexNamespace namespace);
}
