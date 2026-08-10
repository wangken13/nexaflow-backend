package com.vebcoding.trade.file.service;

import com.vebcoding.trade.common.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
public class MinioFileObjectStorage implements FileObjectStorage {
    private final MinioClient minioClient;
    private final String bucket;

    public MinioFileObjectStorage(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @Override
    public void put(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey)
                    .stream(inputStream, size, -1).contentType(contentType).build());
        } catch (Exception exception) {
            throw new BusinessException("文件存储服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public String presignedDownloadUrl(String objectKey, int expiresInSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                    .bucket(bucket).object(objectKey).expiry(expiresInSeconds).build());
        } catch (Exception exception) {
            throw new BusinessException("无法生成文件下载地址，请稍后重试");
        }
    }

    @Override
    public void deleteQuietly(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception ignored) {
            // Metadata persistence remains the source of truth; cleanup is retried by operations tooling.
        }
    }

    private void ensureBucket() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
