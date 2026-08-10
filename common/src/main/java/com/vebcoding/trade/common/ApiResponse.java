package com.vebcoding.trade.common;

public record ApiResponse<T>(boolean success, String code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "ok", data);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return fail(ErrorCode.INVALID_REQUEST, message);
    }

    public static <T> ApiResponse<T> fail(ErrorCode code, String message) {
        return new ApiResponse<>(false, code.name(), message, null);
    }
}
