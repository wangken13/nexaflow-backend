package com.vebcoding.trade.file.service;

import com.vebcoding.trade.common.BusinessException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.common.TextSanitizer;
import com.vebcoding.trade.common.RoleGuard;
import com.vebcoding.trade.file.api.FilePolicyResponse;
import com.vebcoding.trade.file.api.FileUploadResponse;
import com.vebcoding.trade.file.mapper.FileMetadataMapper;
import java.util.UUID;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "webp", "doc", "docx", "xls", "xlsx", "csv", "txt");
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"), Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"), Map.entry("webp", "image/webp"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("csv", "text/csv"), Map.entry("txt", "text/plain"));
    private final FileMetadataMapper fileMetadataMapper;
    private final FileObjectStorage fileObjectStorage;

    @Value("${app.file.bucket:trade-ai}")
    private String bucket;

    @Value("${app.file.endpoint:memory://local}")
    private String endpoint;

    public FileService(FileMetadataMapper fileMetadataMapper) {
        this(fileMetadataMapper, new InMemoryFileObjectStorage());
    }

    @Autowired
    public FileService(FileMetadataMapper fileMetadataMapper, FileObjectStorage fileObjectStorage) {
        this.fileMetadataMapper = fileMetadataMapper;
        this.fileObjectStorage = fileObjectStorage;
    }

    public FileUploadResponse upload(MultipartFile file) {
        RoleGuard.requireAny("OWNER", "ADMIN", "SALES", "OPERATOR");
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要上传的文件，文件内容不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("上传文件不能超过20MB");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BusinessException("无法识别文件名，请重新选择文件");
        }
        String safeName = originalName.replace('\\', '/');
        safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
        if (safeName.isBlank() || safeName.equals(".") || safeName.equals("..")) {
            throw new BusinessException("文件名不合法，请重命名后上传");
        }
        int extensionIndex = safeName.lastIndexOf('.');
        String extension = extensionIndex < 1 ? "" : safeName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 PDF、图片、Office 文档、CSV 或文本文件");
        }
        try (var signatureStream = file.getInputStream()) {
            FileContentValidator.validate(extension, signatureStream.readNBytes(512));
        } catch (IOException exception) {
            throw new BusinessException("读取上传文件失败，请重新选择文件");
        }
        String contentType = CONTENT_TYPES.get(extension);
        String objectKey = TenantContext.tenantId() + "/" + UUID.randomUUID() + "/" + safeName;
        try (var stream = file.getInputStream()) {
            fileObjectStorage.put(objectKey, stream, file.getSize(), contentType);
            FileUploadResponse saved = fileMetadataMapper.save(new FileUploadResponse(objectKey, safeName, file.getSize(), contentType));
            log.info("file.uploaded tenantId={} objectKey={} size={}", TenantContext.tenantId(), objectKey, file.getSize());
            return saved;
        } catch (IOException exception) {
            log.warn("file.upload.read_failed tenantId={}", TenantContext.tenantId());
            throw new BusinessException("读取上传文件失败，请重新选择文件");
        } catch (RuntimeException exception) {
            // Keep object storage and metadata consistent when persistence fails.
            fileObjectStorage.deleteQuietly(objectKey);
            log.warn("file.upload.failed tenantId={} objectKey={}", TenantContext.tenantId(), objectKey, exception);
            throw exception;
        }
    }

    public com.vebcoding.trade.file.api.FileDownloadResponse download(String objectKey) {
        String safeKey = TextSanitizer.required(objectKey, "文件标识");
        FileUploadResponse file = fileMetadataMapper.findByTenantIdAndObjectKey(TenantContext.tenantId(), safeKey)
                .orElseThrow(() -> BusinessException.notFound("文件不存在或无权访问"));
        log.info("file.download_url.issued tenantId={} objectKey={}", TenantContext.tenantId(), safeKey);
        return new com.vebcoding.trade.file.api.FileDownloadResponse(file.objectKey(), file.fileName(),
                fileObjectStorage.presignedDownloadUrl(file.objectKey(), 300), 300);
    }

    public FilePolicyResponse policy() {
        return new FilePolicyResponse(bucket, endpoint);
    }
}
