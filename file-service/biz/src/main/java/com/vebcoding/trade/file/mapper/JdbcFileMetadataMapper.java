package com.vebcoding.trade.file.mapper;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.file.api.FileUploadResponse;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFileMetadataMapper implements FileMetadataMapper {
    private final JdbcTemplate jdbcTemplate;

    public JdbcFileMetadataMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FileUploadResponse save(FileUploadResponse file) {
        jdbcTemplate.update("""
                INSERT INTO file_metadata (object_key, tenant_id, file_name, size_bytes, content_type)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                  file_name = VALUES(file_name),
                  size_bytes = VALUES(size_bytes),
                  content_type = VALUES(content_type)
                """,
                file.objectKey(),
                TenantContext.tenantId(),
                file.fileName(),
                file.size(),
                file.contentType());
        return file;
    }

    @Override
    public Optional<FileUploadResponse> findByTenantIdAndObjectKey(String tenantId, String objectKey) {
        return jdbcTemplate.query("""
                SELECT object_key, file_name, size_bytes, content_type
                FROM file_metadata WHERE tenant_id = ? AND object_key = ?
                """, (rs, rowNum) -> new FileUploadResponse(
                rs.getString("object_key"), rs.getString("file_name"), rs.getLong("size_bytes"),
                rs.getString("content_type")), tenantId, objectKey).stream().findFirst();
    }
}
