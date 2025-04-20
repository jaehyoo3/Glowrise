package com.glowrise.web;

import com.glowrise.service.FileService;
import com.glowrise.service.dto.FileDTO; // downloadFile 에서 사용
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource; // downloadFile 에서 사용
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders; // downloadFile 에서 사용
import org.springframework.http.HttpStatus; // uploadInlineImage 에서 사용
import org.springframework.http.MediaType; // downloadFile 에서 사용
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // uploadInlineImage 에서 사용

import java.io.File; // downloadFile 에서 사용
import java.io.IOException; // uploadInlineImage 에서 사용
import java.util.Map; // uploadInlineImage 에서 사용

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileService fileService;

    @GetMapping("/{id}")
    public ResponseEntity<Resource> getFile(@PathVariable Long id) {
        return fileService.getFileResponse(id);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        FileDTO fileDTO = null;
        try {
            fileDTO = fileService.getFileById(fileId); // DTO 조회
        } catch (EntityNotFoundException e) {
            log.warn("다운로드할 파일을 찾을 수 없습니다 (ID: {}): {}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        }

        File file = new File(fileDTO.getFilePath());
        if (!file.exists() || !file.isFile()) { // 파일 존재 및 실제 파일인지 확인
            log.error("파일 경로에 파일이 없거나 디렉토리입니다: {}", fileDTO.getFilePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String encodedFileName = "";
        try {
            encodedFileName = java.net.URLEncoder.encode(fileDTO.getFileName(), java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        } catch (Exception e) {
            log.error("파일 이름 인코딩 중 오류 발생: {}", fileDTO.getFileName(), e);
            encodedFileName = "downloaded_file"; // 인코딩 실패 시 기본값
        }

        // Content-Type 결정 (DB값 우선)
        String contentType = fileDTO.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream"; // 기본값
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName) // 다운로드 설정
                .body(resource);
    }

    @PostMapping("/upload/inline")
    public ResponseEntity<Map<String, Object>> uploadInlineImage(@RequestParam("image") MultipartFile file) {
        try {
            Map<String, Object> responseData = fileService.uploadInlineImageAndGetIdUrl(file);
            return ResponseEntity.ok(responseData);
        } catch (IllegalArgumentException e) {
            log.warn("인라인 이미지 업로드 실패 (잘못된 인자): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("인라인 이미지 업로드 중 IO 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "이미지 업로드 중 서버 오류 발생"));
        } catch (Exception e) {
            log.error("인라인 이미지 업로드 중 알 수 없는 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "알 수 없는 오류 발생"));
        }
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<?> getFileInfo(@PathVariable Long id) {
        try {
            Object fileInfo = fileService.getFileInfo(id);
            return ResponseEntity.ok(fileInfo);
        } catch (EntityNotFoundException e) {
            log.warn("파일 정보를 찾을 수 없습니다 (ID: {}): {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("파일 정보 조회 중 서버 오류 발생 (ID: {})", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}