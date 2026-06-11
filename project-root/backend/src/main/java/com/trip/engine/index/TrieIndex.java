package com.trip.engine.index;

import java.util.ArrayList;
import java.util.List;

final class TrieIndex {

    private final TrieNode root = new TrieNode();

    TrieIndex(List<IndexDocument> documents) {
        for (IndexDocument document : documents) {
            insert(document);
        }
    }

    List<Long> findByPrefix(String prefix, int limit) {
        if (prefix.isEmpty() || limit <= 0) {
            return List.of();
        }

        TrieNode current = root;
        for (int offset = 0; offset < prefix.length(); ) {
            int codePoint = prefix.codePointAt(offset);
            char[] chars = Character.toChars(codePoint);
            for (char character : chars) {
                current = current.children().get(character);
                if (current == null) {
                    return List.of();
                }
            }
            offset += Character.charCount(codePoint);
        }
        return new ArrayList<>(current.ids()).subList(0, Math.min(limit, current.ids().size()));
    }

    private void insert(IndexDocument document) {
        TrieNode current = root;
        String text = document.text();
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            char[] chars = Character.toChars(codePoint);
            for (char character : chars) {
                current = current.children().computeIfAbsent(character, ignored -> new TrieNode());
                current.ids().add(document.id());
            }
            offset += Character.charCount(codePoint);
        }
    }
}
