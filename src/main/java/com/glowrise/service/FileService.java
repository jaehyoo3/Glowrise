package com.glowrise.service;

import com.glowrise.domain.Post;
import com.glowrise.domain.StoredFile;
import com.glowrise.repository.FileRepository;
import com.glowrise.service.dto.FileDTO;
import com.glowrise.service.mapper.FileMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FileMapper fileMapper;

    @Value("${file.upload-dir}")
    private String uploadDirPath;

    @Transactional(readOnly = true)
    public StoredFile getFileInfo(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("파일 정보를 찾을 수 없습니다 (ID): " + fileId));
    }

    private Resource loadFileAsResourceInternal(StoredFile storedFile) {
        if (storedFile == null || storedFile.getFilePath() == null) {
            throw new IllegalArgumentException("파일 정보 또는 파일 경로가 유효하지 않습니다.");
        }
        try {
            Path filePath = Paths.get(storedFile.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.error("파일 리소스를 찾거나 읽을 수 없음: {}", filePath);
                throw new RuntimeException("지정된 경로에서 파일을 찾거나 읽을 수 없습니다: " + filePath);
            }
        } catch (MalformedURLException ex) {
            log.error("파일 경로 에러 (Malformed URL): {}", storedFile.getFilePath(), ex);
            throw new RuntimeException("파일 경로 오류: " + storedFile.getFileName(), ex);
        } catch (InvalidPathException ex) {
            log.error("파일 경로 에러 (Invalid Path): {}", storedFile.getFilePath(), ex);
            throw new RuntimeException("유효하지 않은 파일 경로입니다: " + storedFile.getFileName(), ex);
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getFileResponse(Long fileId) {
        try {
            StoredFile fileInfo = getFileInfo(fileId);
            Resource resource = loadFileAsResourceInternal(fileInfo);

            String contentType = fileInfo.getContentType();
            if (contentType == null || contentType.isBlank()) {
                log.warn("DB에 Content-Type 정보가 없습니다 (File ID: {}). 기본값 사용.", fileId);
                contentType = "application/octet-stream";
            }

            String originalFileName = fileInfo.getFileName();
            if (originalFileName == null || originalFileName.isBlank()) {
                originalFileName = "downloaded_file";
                log.warn("DB에 원본 파일 이름 정보가 없습니다 (File ID: {}). 기본 파일명 사용.", fileId);
            }

            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            String disposition = "inline; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .body(resource);

        } catch (EntityNotFoundException e) {
            log.warn("요청된 파일을 찾을 수 없습니다 (ID: {}): {}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("파일 반환 중 서버 오류 발생 (File ID: {}): {}", fileId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("파일 반환 중 예상치 못한 오류 발생 (File ID: {}): {}", fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @Transactional
    public StoredFile saveSingleFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("업로드 시도된 파일이 null이거나 비어있습니다.");
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        Path uploadPath = Paths.get(uploadDirPath);
        Files.createDirectories(uploadPath);

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename(), "파일 이름이 null입니다."));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");
        Path destinationPath = uploadPath.resolve(uniqueFilename).normalize();

        StoredFile savedEntity = null;
        try {
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("파일이 디스크에 저장됨: {}", destinationPath);

            StoredFile fileEntity = new StoredFile();
            fileEntity.setFileName(originalFilename);
            fileEntity.setFilePath(destinationPath.toString());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setPost(null);

            savedEntity = fileRepository.save(fileEntity);
            log.info("파일 메타데이터 DB 저장됨 (ID: {})", savedEntity.getId());

            return savedEntity;

        } catch (IOException | DataAccessException e) {
            log.error("파일 업로드 중 오류 발생 (대상 경로: {}). 롤백 시도.", destinationPath, e);
            try {
                Files.deleteIfExists(destinationPath);
                log.info("디스크 파일 생성 롤백됨: {}", destinationPath);
            } catch (IOException ex) {
                log.error("롤백 중 디스크 파일 삭제 실패: {}", destinationPath, ex);
            }
            throw new RuntimeException("파일 업로드 실패.", e);
        }
    }

    @Transactional
    public void deleteFileById(Long fileId) {
        if (fileId == null) {
            log.warn("삭제 요청된 파일 ID가 null입니다.");
            return;
        }
        StoredFile file = getFileInfo(fileId);
        Path filePath = Paths.get(file.getFilePath());
        try {
            boolean deletedFromDisk = Files.deleteIfExists(filePath);
            if (deletedFromDisk) {
                log.info("디스크에서 파일 삭제됨: {}", filePath);
            } else {
                log.warn("디스크에서 파일을 찾을 수 없거나 이미 삭제됨: {}", filePath);
            }
            fileRepository.delete(file);
            log.info("파일 데이터베이스 항목 삭제됨 (ID: {})", fileId);
        } catch (IOException e) {
            log.error("디스크에서 파일 삭제 실패: {}", file.getFilePath(), e);
            try {
                fileRepository.delete(file);
                log.warn("디스크 파일 삭제 실패했으나, DB 레코드 삭제 성공 (ID: {})", fileId);
            } catch (DataAccessException dae) {
                log.error("디스크 파일 삭제 실패 후 DB 레코드 삭제 중 오류 발생 (ID: {}): {}", fileId, dae.getMessage());
            }
        } catch (DataAccessException e) {
            log.error("파일 DB 레코드 삭제 중 오류 발생 (ID: {}): {}", fileId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public FileDTO getFileById(Long fileId) {
        StoredFile storedFile = getFileInfo(fileId);
        return fileMapper.toDto(storedFile);
    }

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

                String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
                String extension = StringUtils.getFilenameExtension(originalFilename);
                String uniqueFilename = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");
                Path destinationPath = uploadPath.resolve(uniqueFilename).normalize();

                Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                createdFilePaths.add(destinationPath);
                log.info("파일이 디스크에 저장됨: {}", destinationPath);

                StoredFile fileEntity = new StoredFile();
                fileEntity.setFileName(originalFilename);
                fileEntity.setFilePath(destinationPath.toString());
                fileEntity.setContentType(file.getContentType());
                fileEntity.setFileSize(file.getSize());
                try {
                    Post postReference = fileMapper.mapPostIdToPostEntity(postId);
                    fileEntity.setPost(postReference);
                } catch (Exception e) {
                    log.error("Post 엔티티 참조 설정 중 오류 발생 (Post ID: {})", postId, e);
                    throw new RuntimeException("게시글 정보 조회 실패 (ID: " + postId + ")", e);
                }

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
            throw new RuntimeException("게시글 파일 업로드 실패 (postId: " + postId + ")", e);
        }
    }

    @Transactional
    public Map<String, Object> uploadInlineImageAndGetIdUrl(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (!isImageFile(file.getContentType())) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        Path uploadPath = Paths.get(uploadDirPath);
        Files.createDirectories(uploadPath);

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + (extension != null ? "." + extension : "");
        Path destinationPath = uploadPath.resolve(uniqueFilename).normalize();

        StoredFile fileEntity = null;
        try {
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("에디터 이미지가 디스크에 저장됨: {}", destinationPath);

            fileEntity = new StoredFile();
            fileEntity.setFileName(originalFilename);
            fileEntity.setFilePath(destinationPath.toString());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setPost(null);

            fileEntity = fileRepository.save(fileEntity);
            log.info("에디터 이미지 메타데이터 DB 저장됨 (ID: {}, PostID: null)", fileEntity.getId());

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/files/")
                    .path(String.valueOf(fileEntity.getId()))
                    .toUriString();

            log.info("생성된 이미지 URL: {}", fileDownloadUri);

            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl", fileDownloadUri);
            result.put("fileId", fileEntity.getId());
            return result;

        } catch (IOException | DataAccessException e) {
            log.error("에디터 이미지 업로드 중 오류 발생. 롤백 시도.", e);
            try {
                Files.deleteIfExists(destinationPath);
                log.info("디스크 파일 생성 롤백됨: {}", destinationPath);
            } catch (IOException ex) {
                log.error("롤백 중 디스크 파일 삭제 실패: {}", destinationPath, ex);
            }
            throw new RuntimeException("에디터 이미지 업로드 실패.", e);
        }
    }

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
                Path filePath = Paths.get(file.getFilePath());
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

        fileRepository.deleteAll(filesToDelete);
        log.info("게시글 ID {}에 대한 {}개의 파일 데이터베이스 항목 삭제됨", postId, filesToDelete.size());

        if (!failedDeletions.isEmpty()) {
            log.error("디스크에서 다음 파일들을 삭제할 수 없었습니다: {}", failedDeletions);
        }
    }


    @Transactional(readOnly = true)
    public List<FileDTO> getFilesByPostId(Long postId) {
        List<StoredFile> files = fileRepository.findByPostId(postId);
        return fileMapper.toDto(files);
    }

    // todo: 배포 후 스케쥴러 작업할 것
    @Transactional
    public void cleanupOrphanFiles() {
        log.info("고아 파일 정리 작업 시작...");
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
                Path filePath = Paths.get(fileEntity.getFilePath());

                if (Files.deleteIfExists(filePath)) {
                    log.info("디스크에서 고아 파일 삭제됨: {}", filePath);
                    fileRepository.deleteById(fileEntity.getId());
                    dbDeleted = true;
                    log.info("DB에서 고아 파일 레코드 삭제됨: ID {}", fileEntity.getId());
                } else {
                    log.warn("디스크에서 고아 파일을 찾을 수 없거나 이미 삭제됨: {}", filePath);
                    fileRepository.deleteById(fileEntity.getId());
                    dbDeleted = true;
                    log.warn("DB에서 고아 파일 레코드 삭제됨 (디스크 파일 없음): ID {}", fileEntity.getId());
                }
            } catch (IOException e) {
                log.error("고아 파일 디스크 삭제 중 오류 발생 (파일 경로: {}): {}", fileEntity.getFilePath(), e.getMessage());
                failedPhysicalDeletions.add(fileEntity.getFilePath());
            } catch (DataAccessException e) {
                log.error("고아 파일 DB 레코드 삭제 중 오류 발생 (ID: {}): {}", fileEntity.getId(), e.getMessage());
            }
        }

        if (!failedPhysicalDeletions.isEmpty()) {
            log.error("다음 고아 파일들의 물리적 삭제를 실패했습니다: {}", failedPhysicalDeletions);
        }
        log.info("고아 파일 정리 작업 완료.");
    }

    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
}