package com.vebcoding.trade.file.api;

public record FileDownloadResponse(String objectKey, String fileName, String downloadUrl, int expiresInSeconds) {
}
