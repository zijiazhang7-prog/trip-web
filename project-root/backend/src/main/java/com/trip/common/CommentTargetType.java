package com.trip.common;

import java.util.Locale;

/**
 * 评论目标类型。
 */
public enum CommentTargetType {
    DESTINATION,
    FOOD,
    DIARY;

    public static CommentTargetType fromPath(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public String pathValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
