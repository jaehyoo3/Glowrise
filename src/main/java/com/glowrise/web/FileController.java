package com.glowrise.web;

import com.glowrise.service.FileService;
import com.glowrise.service.dto.FileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/{fileId}")
    public ResponseEntity<FileDTO> getFileById(@PathVariable Long fileId) {
        FileDTO fileDTO = fileService.getFileById(fileId);
        return ResponseEntity.ok(fileDTO);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        FileDTO fileDTO = fileService.getFileById(fileId);
        File file = new File(fileDTO.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String encodedFileName = java.net.URLEncoder.encode(fileDTO.getFileName(), java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileDTO.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @PostMapping("/upload/inline")
    public ResponseEntity<Map<String, Object>> uploadInlineImage(@RequestParam("image") MultipartFile file) {
        try {
            Map<String, Object> responseData = fileService.uploadInlineImageAndGetIdUrl(file);
            return ResponseEntity.ok(responseData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "이미지 업로드 중 서버 오류 발생"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "알 수 없는 오류 발생"));
        }
    }
}