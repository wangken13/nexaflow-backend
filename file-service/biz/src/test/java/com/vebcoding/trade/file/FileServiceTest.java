package com.vebcoding.trade.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.AccessDeniedException;
import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.file.mapper.InMemoryFileMetadataMapper;
import com.vebcoding.trade.file.service.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileServiceTest {
    private FileService service;

    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("tenant-file-test");
        TenantContext.setUserId("operator-1");
        TenantContext.setRole("OPERATOR");
        service = new FileService(new InMemoryFileMetadataMapper());
        ReflectionTestUtils.setField(service, "bucket", "nexaflow-files");
        ReflectionTestUtils.setField(service, "endpoint", "http://minio.test:9000");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void uploadStoresTenantScopedObjectMetadata() {
        var file = new MockMultipartFile("file", "quote.pdf", "application/pdf", new byte[] {1, 2, 3});

        var uploaded = service.upload(file);

        assertThat(uploaded.objectKey()).startsWith("tenant-file-test/");
        assertThat(uploaded.fileName()).isEqualTo("quote.pdf");
        assertThat(uploaded.size()).isEqualTo(3);
    }

    @Test
    void uploadRejectsReadOnlyMember() {
        TenantContext.setRole("VIEWER");
        var file = new MockMultipartFile("file", "contract.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("当前账号没有执行此操作的权限");
    }

    @Test
    void policyExposesConfiguredStorageLocation() {
        assertThat(service.policy())
                .extracting("bucket", "endpoint")
                .containsExactly("nexaflow-files", "http://minio.test:9000");
    }
}
