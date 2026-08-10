package com.vebcoding.trade.common;

public enum ErrorCode {
    INVALID_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    PAYLOAD_TOO_LARGE(413),
    RATE_LIMITED(429),
    UPSTREAM_ERROR(502),
    SERVICE_UNAVAILABLE(503),
    REQUEST_TIMEOUT(504),
    INTERNAL_ERROR(500);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }
}
