package com.vebcoding.trade.file.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true")
public class MinioConfig {
    @Bean
    MinioClient minioClient(@Value("${minio.endpoint}") String endpoint,
                            @Value("${minio.access-key}") String accessKey,
                            @Value("${minio.secret-key}") String secretKey) {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}
