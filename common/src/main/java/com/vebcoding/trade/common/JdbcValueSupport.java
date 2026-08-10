package com.vebcoding.trade.common;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class JdbcValueSupport {
    private JdbcValueSupport() {
    }

    public static String stringOrEmpty(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? "" : value;
    }

    public static String timestampToIso(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? "" : value.toInstant().toString();
    }

    public static Timestamp isoToTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Timestamp.from(Instant.parse(trimmed));
        } catch (RuntimeException ignored) {
            return Timestamp.valueOf(normalizeLocalDateTime(trimmed));
        }
    }

    public static Date isoToDate(String value) {
        return Date.valueOf(LocalDate.parse(value.trim()));
    }

    public static BigDecimal decimal(Number value) {
        return BigDecimal.valueOf(value.doubleValue());
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static LocalDateTime normalizeLocalDateTime(String value) {
        String normalized = value.replace('T', ' ');
        int dotIndex = normalized.indexOf('.');
        if (dotIndex >= 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.length() == 10) {
            normalized = normalized + " 00:00:00";
        }
        if (normalized.length() > 19) {
            normalized = normalized.substring(0, 19);
        }
        return LocalDateTime.parse(normalized.replace(' ', 'T'));
    }
}
