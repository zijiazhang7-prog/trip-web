package com.trip.engine.index;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 基于 Hash、Trie 和字符位置倒排结构的内存文本索引。
 */
@Component
public class IndexEngine {

    private final AtomicReference<Map<IndexNamespace, IndexSnapshot>> snapshots =
            new AtomicReference<>(Map.of());

    /**
     * 构建指定命名空间并原子替换旧快照。
     */
    public void rebuild(IndexNamespace namespace, List<IndexDocument> documents) {
        if (namespace == null) {
            return;
        }

        List<IndexDocument> normalizedDocuments = normalizeDocuments(documents);
        IndexSnapshot snapshot = namespace == IndexNamespace.DIARY_CONTENT
                ? IndexSnapshot.inverted(new InvertedIndex(normalizedDocuments))
                : IndexSnapshot.name(new HashIndex(normalizedDocuments), new TrieIndex(normalizedDocuments));
        snapshots.updateAndGet(current -> {
            Map<IndexNamespace, IndexSnapshot> updated = new EnumMap<>(IndexNamespace.class);
            updated.putAll(current);
            updated.put(namespace, snapshot);
            return Map.copyOf(updated);
        });
    }

    public IndexSearchResult findExact(IndexNamespace namespace, String value) {
        IndexSnapshot snapshot = snapshots.get().get(namespace);
        if (snapshot == null || snapshot.hashIndex() == null) {
            return IndexSearchResult.unavailable();
        }
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return IndexSearchResult.available(List.of());
        }
        return IndexSearchResult.available(snapshot.hashIndex().find(normalized));
    }

    public IndexSearchResult findByPrefix(IndexNamespace namespace, String prefix, int limit) {
        IndexSnapshot snapshot = snapshots.get().get(namespace);
        if (snapshot == null || snapshot.trieIndex() == null) {
            return IndexSearchResult.unavailable();
        }
        String normalized = normalize(prefix);
        if (!StringUtils.hasText(normalized) || limit <= 0) {
            return IndexSearchResult.available(List.of());
        }
        return IndexSearchResult.available(snapshot.trieIndex().findByPrefix(normalized, limit));
    }

    /**
     * 使用字符位置倒排索引查找包含完整连续关键词的文档。
     */
    public IndexSearchResult findByContent(IndexNamespace namespace, String keyword) {
        IndexSnapshot snapshot = snapshots.get().get(namespace);
        if (snapshot == null || snapshot.invertedIndex() == null) {
            return IndexSearchResult.unavailable();
        }
        String normalized = normalize(keyword);
        if (!StringUtils.hasText(normalized)) {
            return IndexSearchResult.available(List.of());
        }
        return IndexSearchResult.available(snapshot.invertedIndex().findContaining(normalized));
    }

    /**
     * 增量新增或替换正文文档。索引不可用时不创建不完整快照。
     */
    public void upsert(IndexNamespace namespace, IndexDocument document) {
        if (namespace != IndexNamespace.DIARY_CONTENT
                || document == null
                || document.id() == null
                || document.id() <= 0) {
            return;
        }
        String normalized = normalize(document.text());
        if (!StringUtils.hasText(normalized)) {
            remove(namespace, document.id());
            return;
        }

        snapshots.updateAndGet(current -> {
            IndexSnapshot existing = current.get(namespace);
            if (existing == null || existing.invertedIndex() == null) {
                return current;
            }
            Map<IndexNamespace, IndexSnapshot> updated = new EnumMap<>(IndexNamespace.class);
            updated.putAll(current);
            updated.put(
                    namespace,
                    IndexSnapshot.inverted(existing.invertedIndex()
                            .upsert(new IndexDocument(document.id(), normalized))));
            return Map.copyOf(updated);
        });
    }

    /**
     * 增量删除正文文档。索引不可用时保持不可用状态。
     */
    public void remove(IndexNamespace namespace, Long documentId) {
        if (namespace != IndexNamespace.DIARY_CONTENT || documentId == null || documentId <= 0) {
            return;
        }
        snapshots.updateAndGet(current -> {
            IndexSnapshot existing = current.get(namespace);
            if (existing == null || existing.invertedIndex() == null) {
                return current;
            }
            Map<IndexNamespace, IndexSnapshot> updated = new EnumMap<>(IndexNamespace.class);
            updated.putAll(current);
            updated.put(
                    namespace,
                    IndexSnapshot.inverted(existing.invertedIndex().remove(documentId)));
            return Map.copyOf(updated);
        });
    }

    public boolean isReady(IndexNamespace namespace) {
        return namespace != null && snapshots.get().containsKey(namespace);
    }

    public void invalidate(IndexNamespace namespace) {
        if (namespace == null) {
            return;
        }
        snapshots.updateAndGet(current -> {
            Map<IndexNamespace, IndexSnapshot> updated = new EnumMap<>(IndexNamespace.class);
            updated.putAll(current);
            updated.remove(namespace);
            return Map.copyOf(updated);
        });
    }

    private List<IndexDocument> normalizeDocuments(List<IndexDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        Map<Long, IndexDocument> uniqueDocuments = new LinkedHashMap<>();
        for (IndexDocument document : documents) {
            if (document == null || document.id() == null || document.id() <= 0) {
                continue;
            }
            String normalized = normalize(document.text());
            if (StringUtils.hasText(normalized)) {
                uniqueDocuments.put(document.id(), new IndexDocument(document.id(), normalized));
            }
        }
        return new ArrayList<>(uniqueDocuments.values());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private record IndexSnapshot(
            HashIndex hashIndex,
            TrieIndex trieIndex,
            InvertedIndex invertedIndex) {

        private static IndexSnapshot name(HashIndex hashIndex, TrieIndex trieIndex) {
            return new IndexSnapshot(hashIndex, trieIndex, null);
        }

        private static IndexSnapshot inverted(InvertedIndex invertedIndex) {
            return new IndexSnapshot(null, null, invertedIndex);
        }
    }
}
