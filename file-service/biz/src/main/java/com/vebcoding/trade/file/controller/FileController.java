package com.vebcoding.trade.file.controller;

import com.vebcoding.trade.common.ApiResponse;
import com.vebcoding.trade.file.api.FilePolicyResponse;
import com.vebcoding.trade.file.api.FileUploadResponse;
import com.vebcoding.trade.file.api.FileDownloadResponse;
import com.vebcoding.trade.file.service.FileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(fileService.upload(file));
    }

    @GetMapping("/policy")
    public ApiResponse<FilePolicyResponse> policy() {
        return ApiResponse.ok(fileService.policy());
    }

    @GetMapping("/download")
    public ApiResponse<FileDownloadResponse> download(@RequestParam("objectKey") String objectKey) {
        return ApiResponse.ok(fileService.download(objectKey));
    }
}
