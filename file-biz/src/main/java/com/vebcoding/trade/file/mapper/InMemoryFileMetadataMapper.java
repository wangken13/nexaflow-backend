package com.vebcoding.trade.file.mapper;

import com.vebcoding.trade.file.api.FileUploadResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFileMetadataMapper implements FileMetadataMapper {
    private final List<FileUploadResponse> files = new CopyOnWriteArrayList<>();

    @Override
    public FileUploadResponse save(FileUploadResponse file) {
        files.add(file);
        return file;
    }
}