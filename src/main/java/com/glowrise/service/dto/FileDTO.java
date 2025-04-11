package com.glowrise.service.dto;

import lombok.Data;

@Data
public class FileDTO {
    private Long id;
    private String fileName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private Long postId;
}