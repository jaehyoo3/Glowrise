package com.glowrise.web;

import com.glowrise.service.FileService;
import com.glowrise.service.dto.FileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        // 중요: fileDTO.getFilePath()가 실제 파일 시스템 경로여야 함
        File file = new File(fileDTO.getFilePath());
        if (!file.exists()) {
            log.warn("다운로드 요청 파일 없음: {}", fileDTO.getFilePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        // 파일 이름 인코딩 처리 고려 (브라우저 호환성)
        String encodedFileName = java.net.URLEncoder.encode(fileDTO.getFileName(), java.nio.charset.StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileDTO.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName) // RFC 5987 방식
                .body(resource);
    }

    /**
     * 텍스트 에디터에서 인라인 이미지 업로드를 처리하는 엔드포인트.
     *
     * @param file 업로드된 이미지 파일 (파라미터 이름: 'image')
     * @return JSON 형식으로 {"imageUrl": "생성된 이미지 URL"} 또는 에러 메시지
     */
    @PostMapping("/upload/inline")
    // 반환 타입을 Map<String, Object> 로 변경 (String과 Long을 모두 담기 위해)
    public ResponseEntity<Map<String, Object>> uploadInlineImage(@RequestParam("image") MultipartFile file) {
        log.info("인라인 이미지 업로드 요청 받음: {}", file.getOriginalFilename());
        try {
            // *** 수정: FileService의 ID와 URL을 반환하는 메소드 호출 ***
            Map<String, Object> responseData = fileService.uploadInlineImageAndGetIdUrl(file);
            log.info("인라인 이미지 업로드 성공: Data={}", responseData);
            // *** 수정: 서비스로부터 받은 Map 객체를 그대로 반환 ***
            return ResponseEntity.ok(responseData);
        } catch (IllegalArgumentException e) {
            log.warn("인라인 이미지 업로드 실패 (Bad Request): {}", e.getMessage());
            // 에러 응답도 일관성을 위해 Map 사용 (선택 사항)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("인라인 이미지 업로드 실패 (Server Error)", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "이미지 업로드 중 서버 오류 발생"));
        } catch (Exception e) {
            log.error("인라인 이미지 업로드 실패 (Unknown Error)", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "알 수 없는 오류 발생"));
        }
    }
}