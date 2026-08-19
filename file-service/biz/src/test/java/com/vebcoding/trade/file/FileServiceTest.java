package com.vebcoding.trade.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vebcoding.trade.common.AccessDeniedException;
import com.vebcoding.trade.common.BusinessException;
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
        var file = new MockMultipartFile("file", "quote.pdf", "text/html", "%PDF-1.7".getBytes());

        var uploaded = service.upload(file);

        assertThat(uploaded.objectKey()).startsWith("tenant-file-test/");
        assertThat(uploaded.fileName()).isEqualTo("quote.pdf");
        assertThat(uploaded.size()).isEqualTo(8);
        assertThat(uploaded.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchExtension() {
        var file = new MockMultipartFile("file", "contract.pdf", "application/pdf",
                "<script>alert(1)</script>".getBytes());

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件内容与扩展名不匹配");
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

    @Test
    void uploadRejectsFileAboveServiceLimit() {
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.when(file.isEmpty()).thenReturn(false);
        org.mockito.Mockito.when(file.getSize()).thenReturn(20L * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(com.vebcoding.trade.common.BusinessException.class)
                .hasMessage("上传文件不能超过20MB");
    }
}
