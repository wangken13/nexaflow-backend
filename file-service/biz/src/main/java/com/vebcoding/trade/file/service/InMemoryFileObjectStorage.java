package com.vebcoding.trade.file.service;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Development-only storage strategy used when MinIO has not been configured. */
@Component
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "false", matchIfMissing = true)
public final class InMemoryFileObjectStorage implements FileObjectStorage {
    private static final Logger log = LoggerFactory.getLogger(InMemoryFileObjectStorage.class);

    public InMemoryFileObjectStorage() {
        log.warn("file.storage.memory_mode_enabled files are not durable; configure MINIO_ENABLED=true for deployed environments");
    }

    @Override public void put(String objectKey, InputStream inputStream, long size, String contentType) { }
    @Override public String presignedDownloadUrl(String objectKey, int expiresInSeconds) { return "memory://" + objectKey; }
    @Override public void deleteQuietly(String objectKey) { }
}
