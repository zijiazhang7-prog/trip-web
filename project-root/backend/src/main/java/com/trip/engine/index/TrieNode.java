package com.trip.engine.index;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class TrieNode {

    private final Map<Character, TrieNode> children = new LinkedHashMap<>();
    private final Set<Long> ids = new LinkedHashSet<>();

    Map<Character, TrieNode> children() {
        return children;
    }

    Set<Long> ids() {
        return ids;
    }
}
