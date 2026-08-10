package com.vebcoding.trade.file.api;

public record FileUploadResponse(String objectKey, String fileName, long size, String contentType) {
}
