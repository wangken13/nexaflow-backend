package com.vebcoding.trade.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.vebcoding.trade.common.TenantContext;
import com.vebcoding.trade.file.controller.FileController;
import com.vebcoding.trade.file.mapper.InMemoryFileMetadataMapper;
import com.vebcoding.trade.file.service.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileControllerTest {
    @BeforeEach
    void authenticate() {
        TenantContext.setTenantId("tenant-file-test");
        TenantContext.setUserId("sales-1");
        TenantContext.setRole("SALES");
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void uploadReturnsSuccessfulApiResponse() {
        FileController controller = new FileController(new FileService(new InMemoryFileMetadataMapper()));
        var file = new MockMultipartFile("file", "inquiry.txt", "text/plain", "hello".getBytes());

        var response = controller.upload(file);

        assertThat(response.success()).isTrue();
        assertThat(response.data().fileName()).isEqualTo("inquiry.txt");
    }
}
