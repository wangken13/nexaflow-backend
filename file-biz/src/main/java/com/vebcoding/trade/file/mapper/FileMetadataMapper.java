package com.vebcoding.trade.file.mapper;

import com.vebcoding.trade.file.api.FileUploadResponse;

public interface FileMetadataMapper {
    FileUploadResponse save(FileUploadResponse file);
}