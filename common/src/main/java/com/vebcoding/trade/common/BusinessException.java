package com.vebcoding.trade.common;

public class BusinessException extends RuntimeException {
    private final ErrorCode code;

    public BusinessException(String message) {
        this(ErrorCode.INVALID_REQUEST, message);
    }

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(ErrorCode.UNAUTHORIZED, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }

    public static BusinessException rateLimited(String message) {
        return new BusinessException(ErrorCode.RATE_LIMITED, message);
    }

    public static BusinessException upstream(String message) {
        return new BusinessException(ErrorCode.UPSTREAM_ERROR, message);
    }

    public static BusinessException unavailable(String message) {
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, message);
    }
}
