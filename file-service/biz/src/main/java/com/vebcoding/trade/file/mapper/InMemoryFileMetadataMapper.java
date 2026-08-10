package com.vebcoding.trade.file.mapper;

import com.vebcoding.trade.file.api.FileUploadResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryFileMetadataMapper implements FileMetadataMapper {
    private final List<FileUploadResponse> files = new CopyOnWriteArrayList<>();

    @Override
    public FileUploadResponse save(FileUploadResponse file) {
        files.add(file);
        return file;
    }

    @Override
    public Optional<FileUploadResponse> findByTenantIdAndObjectKey(String tenantId, String objectKey) {
        return files.stream().filter(file -> file.objectKey().equals(objectKey)
                && file.objectKey().startsWith(tenantId + "/")).findFirst();
    }
}
