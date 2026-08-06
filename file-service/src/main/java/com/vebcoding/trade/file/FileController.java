package com.vebcoding.trade.file;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.common.TenantContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {
    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        String objectKey = TenantContext.tenantId() + "/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
        return ApiResponse.ok(Map.of("objectKey", objectKey, "fileName", file.getOriginalFilename(), "size", file.getSize()));
    }

    @GetMapping("/policy")
    public ApiResponse<Map<String, String>> policy() {
        return ApiResponse.ok(Map.of("bucket", "trade-ai", "endpoint", "${MINIO_ENDPOINT:http://localhost:9000}"));
    }
}
