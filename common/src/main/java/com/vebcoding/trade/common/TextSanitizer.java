package com.vebcoding.trade.common;

public final class TextSanitizer {
    private TextSanitizer() {
    }

    public static String required(String value, String fieldName) {
        String normalized = optional(value);
        if (normalized.isBlank()) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return normalized;
    }

    public static String optional(String value) {
        return value == null ? "" : value.trim();
    }
}
