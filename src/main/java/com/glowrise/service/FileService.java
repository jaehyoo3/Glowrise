package com.glowrise.service;

import com.glowrise.domain.StoredFile;
import com.glowrise.repository.FileRepository;
import com.glowrise.service.dto.FileDTO;
import com.glowrise.service.mapper.FileMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {
    private final FileRepository fileRepository;
    private final FileMapper fileMapper;

    @Value("${file.upload-dir}") // 설정된 업로드 디렉토리 경로 주입
    private String uploadDirPath;

    @Transactional
    public List<FileDTO> uploadFiles(List<MultipartFile> multipartFiles, Long postId) throws IOException {
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return new ArrayList<>();
        }
        Path uploadPath = Paths.get(uploadDirPath);
        Files.createDirectories(uploadPath);

        List<StoredFile> savedEntities = new ArrayList<>();
        List<Path> createdFilePaths = new ArrayList<>();

        try {
            for (MultipartFile file : multipartFiles) {
                if (file == null || file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                String extension = "";
                int dotIndex = originalFilename.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = originalFilename.substring(dotIndex);
                }
                String uniqueFilename = UUID.randomUUID().toString() + extension; // 고유 파일명 사용
                Path destinationPath = uploadPath.resolve(uniqueFilename);

                // 파일 디스크에 저장
                Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                createdFilePaths.add(destinationPath);
                log.info("파일이 디스크에 저장됨: {}", destinationPath);

                // File 엔티티 생성
                StoredFile fileEntity = new StoredFile();
                fileEntity.setFileName(originalFilename);
                fileEntity.setFilePath(destinationPath.toString()); // 엔티티에는 전체 경로 저장
                fileEntity.setContentType(file.getContentType());
                fileEntity.setFileSize(file.getSize());
                // Post와 연결 (매퍼 사용)
                fileEntity.setPost(fileMapper.mapPostIdToPostEntity(postId));

                // 데이터베이스에 엔티티 저장
                savedEntities.add(fileRepository.save(fileEntity));
            }
            log.info("게시글 {}에 {}개의 파일 업로드 성공", postId, savedEntities.size());
            return fileMapper.toDto(savedEntities);

        } catch (IOException | DataAccessException e) {
            log.error("게시글 {} 파일 업로드 중 오류 발생. 롤백 시도.", postId, e);
            for (Path path : createdFilePaths) {
                try {
                    Files.deleteIfExists(path);
                    log.info("파일 생성 롤백됨: {}", path);
                } catch (IOException ex) {
                    log.error("롤백 중 파일 삭제 실패: {}", path, ex);
                }
            }
            throw e; // 예외 다시 던지기
        }
    }

    /**
     * 에디터 내 삽입용 이미지를 저장하고, DB에 메타데이터 저장 후 웹 접근 URL을 반환합니다.
     * DB 저장 시 postId는 null입니다.
     *
     * @param file 업로드된 이미지 파일
     * @return 웹에서 접근 가능한 이미지 URL
     * @throws IOException 파일 저장 실패 시
     */
    @Transactional // 파일 저장 및 DB 저장을 한 트랜잭션으로 묶음
    public Map<String, Object> uploadInlineImageAndGetIdUrl(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (!isImageFile(file.getContentType())) { // 이미지 파일인지 간단히 확인
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        Path uploadPath = Paths.get(uploadDirPath);
        Files.createDirectories(uploadPath); // 디렉토리 없으면 생성

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex); // 예: ".jpg"
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension; // UUID + 확장자 형태의 고유 파일명
        Path destinationPath = uploadPath.resolve(uniqueFilename);

        StoredFile fileEntity = null; // DB 저장 위한 엔티티 변수
        try {
            // 1. 파일 디스크에 저장
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("에디터 이미지가 디스크에 저장됨: {}", destinationPath);

            // 2. StoredFile 엔티티 생성 및 DB 저장 (postId는 null)
            fileEntity = new StoredFile(); // *** 엔티티 이름 StoredFile로 변경 가정 ***
            fileEntity.setFileName(originalFilename);
            fileEntity.setFilePath(destinationPath.toString()); // 전체 경로 저장
            fileEntity.setContentType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setPost(null); // postId가 아직 없으므로 null로 설정

            fileEntity = fileRepository.save(fileEntity); // DB에 저장하고 ID 할당받음
            log.info("에디터 이미지 메타데이터 DB 저장됨 (ID: {}, PostID: null)", fileEntity.getId());

            // 3. 웹 접근 URL 생성 및 반환
            // WebConfig에서 /uploads/** 경로로 매핑했다고 가정
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/") // WebConfig에 설정된 리소스 핸들러 경로
                    .path(uniqueFilename) // 고유 파일명 사용
                    .toUriString();

            log.info("생성된 이미지 URL: {}", fileDownloadUri);

            // 4. URL과 파일 ID를 Map에 담아 반환
            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", fileDownloadUri);
            result.put("fileId", fileEntity.getId()); // 저장된 엔티티의 ID 추가
            return result;

        } catch (IOException | DataAccessException e) { // 파일 저장 또는 DB 저장 실패 시
            log.error("에디터 이미지 업로드 중 오류 발생. 롤백 시도.", e);
            // 롤백: 파일이 생성되었다면 삭제
            try {
                Files.deleteIfExists(destinationPath);
                log.info("디스크 파일 생성 롤백됨: {}", destinationPath);
            } catch (IOException ex) {
                log.error("롤백 중 디스크 파일 삭제 실패: {}", destinationPath, ex);
            }
            // DB 저장이 이미 되었다면 트랜잭션 롤백 (@Transactional에 의해 처리됨)
            throw e; // 원본 예외 다시 던지기
        }
    }

    /**
     * 게시글 ID와 연관된 모든 파일 및 DB 레코드를 삭제합니다.
     * (기존 코드 유지)
     */
    @Transactional
    public void deleteFilesByPostId(Long postId) {
        List<StoredFile> filesToDelete = fileRepository.findByPostId(postId);
        if (filesToDelete.isEmpty()) {
            log.info("게시글 ID {}에 대해 삭제할 파일 없음", postId);
            return;
        }

        List<String> failedDeletions = new ArrayList<>();
        for (StoredFile file : filesToDelete) {
            try {
                Path filePath = Paths.get(file.getFilePath()); // DB에 저장된 전체 경로 사용
                if (Files.deleteIfExists(filePath)) {
                    log.info("디스크에서 파일 삭제됨: {}", filePath);
                } else {
                    log.warn("디스크에서 파일을 찾을 수 없거나 이미 삭제됨: {}", filePath);
                }
            } catch (IOException e) {
                log.error("디스크에서 파일 삭제 실패: {}", file.getFilePath(), e);
                failedDeletions.add(file.getFilePath());
            }
        }

        // DB 레코드 삭제 (물리 파일 삭제 성공 여부와 관계 없이 일단 삭제)
        fileRepository.deleteAll(filesToDelete);
        log.info("게시글 ID {}에 대한 {}개의 파일 데이터베이스 항목 삭제됨", postId, filesToDelete.size());

        if (!failedDeletions.isEmpty()) {
            log.error("디스크에서 다음 파일들을 삭제할 수 없었습니다: {}", failedDeletions);
            // 실패 로깅 외 추가 조치 필요 시 구현
        }
    }

    /**
     * ID로 파일 메타데이터를 조회합니다.
     * (기존 코드 유지)
     */
    @Transactional(readOnly = true)
    public FileDTO getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .map(fileMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("파일을 찾을 수 없습니다 (ID): " + fileId));
    }

    /**
     * 게시글 ID로 연관된 파일 메타데이터 목록을 조회합니다.
     * (기존 코드 유지)
     */
    @Transactional(readOnly = true)
    public List<FileDTO> getFilesByPostId(Long postId) {
        List<StoredFile> files = fileRepository.findByPostId(postId);
        return fileMapper.toDto(files);
    }

    // --- 고아 파일 정리 로직 (추가) ---

    /**
     * 특정 Post에 연결되지 않고 일정 시간 이상 경과된 '고아' 파일 레코드 및
     * 해당 물리적 파일을 삭제하는 스케줄링 메소드.
     * (@Scheduled 어노테이션은 별도 스케줄러 클래스나 설정 클래스에서 호출)
     */
    @Transactional
    public void cleanupOrphanFiles() {
        log.info("고아 파일 정리 작업 시작...");
        // 예: 생성된 지 24시간이 지났고 postId가 null인 파일 조회
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<StoredFile> orphanFileEntities = fileRepository.findByPostIsNullAndCreatedDateBefore(threshold);

        if (orphanFileEntities.isEmpty()) {
            log.info("정리할 고아 파일 없음.");
            return;
        }

        log.info("{}개의 고아 파일 정리 시도.", orphanFileEntities.size());
        List<String> failedPhysicalDeletions = new ArrayList<>();

        for (StoredFile fileEntity : orphanFileEntities) {
            boolean dbDeleted = false;
            try {
                // DB에 저장된 전체 경로 사용
                Path filePath = Paths.get(fileEntity.getFilePath());

                if (Files.deleteIfExists(filePath)) {
                    log.info("디스크에서 고아 파일 삭제됨: {}", filePath);
                    // 물리 파일 삭제 성공 시 DB 레코드 삭제
                    fileRepository.deleteById(fileEntity.getId());
                    dbDeleted = true;
                    log.info("DB에서 고아 파일 레코드 삭제됨: ID {}", fileEntity.getId());
                } else {
                    log.warn("디스크에서 고아 파일을 찾을 수 없거나 이미 삭제됨: {}", filePath);
                    // 디스크에 파일 없어도 DB 레코드는 삭제
                    fileRepository.deleteById(fileEntity.getId());
                    dbDeleted = true;
                    log.warn("DB에서 고아 파일 레코드 삭제됨 (디스크 파일 없음): ID {}", fileEntity.getId());
                }
            } catch (IOException e) { // 디스크 파일 삭제 오류
                log.error("고아 파일 디스크 삭제 중 오류 발생 (파일 경로: {}): {}", fileEntity.getFilePath(), e.getMessage());
                failedPhysicalDeletions.add(fileEntity.getFilePath());
                // 디스크 삭제 실패 시 DB 레코드 삭제 여부 결정 (여기서는 삭제 시도 안함)
            } catch (DataAccessException e) { // DB 삭제 오류
                log.error("고아 파일 DB 레코드 삭제 중 오류 발생 (ID: {}): {}", fileEntity.getId(), e.getMessage());
                // 이미 디스크 파일이 삭제된 경우 문제가 될 수 있으므로 로깅 중요
            }
        }

        if (!failedPhysicalDeletions.isEmpty()) {
            log.error("다음 고아 파일들의 물리적 삭제를 실패했습니다: {}", failedPhysicalDeletions);
            // 실패 알림 등 추가 조치 고려
        }
        log.info("고아 파일 정리 작업 완료.");
    }

    // 이미지 파일 타입 확인 헬퍼 메소드
    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}