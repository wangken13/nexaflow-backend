package com.vebcoding.trade.file.service;

import java.io.InputStream;

public interface FileObjectStorage {
    void put(String objectKey, InputStream inputStream, long size, String contentType);

    String presignedDownloadUrl(String objectKey, int expiresInSeconds);

    void deleteQuietly(String objectKey);
}
