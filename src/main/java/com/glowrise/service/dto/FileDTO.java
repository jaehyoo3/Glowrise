package com.glowrise.service.dto;

import lombok.Data;

@Data
public class FileDTO {
    private Long id;
    private String fileName; // 원본 파일 이름
    private String filePath; // 저장된 파일 경로
    private String contentType; // MIME 타입
    private Long fileSize; // 파일 크기
    private Long postId; // 연관된 게시글 ID (nullable)
}