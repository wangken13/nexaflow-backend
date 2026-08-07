package com.vebcoding.trade.file.service;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.file.api.FilePolicyResponse;
import com.vebcoding.trade.file.api.FileUploadResponse;
import com.vebcoding.trade.file.mapper.FileMetadataMapper;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    private final FileMetadataMapper fileMetadataMapper;

    @Value("${app.file.bucket:trade-ai}")
    private String bucket;

    @Value("${app.file.endpoint:${MINIO_ENDPOINT:http://localhost:9000}}")
    private String endpoint;

    public FileService(FileMetadataMapper fileMetadataMapper) {
        this.fileMetadataMapper = fileMetadataMapper;
    }

    public FileUploadResponse upload(MultipartFile file) {
        String objectKey = TenantContext.tenantId() + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        return fileMetadataMapper.save(new FileUploadResponse(objectKey, file.getOriginalFilename(), file.getSize()));
    }

    public FilePolicyResponse policy() {
        return new FilePolicyResponse(bucket, endpoint);
    }
}