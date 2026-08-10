package com.vebcoding.trade.file.mapper;

import com.vebcoding.trade.file.api.FileUploadResponse;
import java.util.Optional;

public interface FileMetadataMapper {
    FileUploadResponse save(FileUploadResponse file);

    Optional<FileUploadResponse> findByTenantIdAndObjectKey(String tenantId, String objectKey);
}
