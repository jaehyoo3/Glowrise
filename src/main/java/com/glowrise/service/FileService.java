package com.glowrise.service;

import com.glowrise.domain.Files;
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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FileMapper fileMapper;
    // 선택 사항: 파일 저장 전 postId 유효성 검사가 필요한 경우 PostRepository 주입
    // private final PostRepository postRepository;

    @Value("${file.upload-dir}") // 설정된 업로드 디렉토리 경로 주입
    private String uploadDirPath;

    @Transactional // 파일 업로드 및 DB 저장을 (부분적으로) 트랜잭션으로 만듬
    public List<FileDTO> uploadFiles(List<MultipartFile> multipartFiles, Long postId) throws IOException {
        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return new ArrayList<>();
        }

        // 선택 사항: Post 존재 여부 확인
        // findPostByIdOrThrow(postId); // PostRepository가 주입된 경우 주석 해제

        Path uploadPath = Paths.get(uploadDirPath);
        java.nio.file.Files.createDirectories(uploadPath); // NIO를 사용하여 디렉토리 존재 확인 및 생성

        List<Files> savedEntities = new ArrayList<>();
        List<Path> createdFilePaths = new ArrayList<>(); // 잠재적 롤백을 위해 생성된 파일 추적

        try {
            for (MultipartFile file : multipartFiles) {
                if (file == null || file.isEmpty()) continue;

                String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
                String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;
                Path destinationPath = uploadPath.resolve(uniqueFilename);

                // 파일 디스크에 저장
                java.nio.file.Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                createdFilePaths.add(destinationPath); // 성공적으로 저장된 파일 경로 추적
                log.info("파일이 디스크에 저장됨: {}", destinationPath);

                // File 엔티티 생성 (FileDTO가 전달되면 매퍼 사용, 아니면 수동 생성)
                Files fileEntity = new Files();
                fileEntity.setFileName(originalFilename);
                fileEntity.setFilePath(destinationPath.toString()); // 전체 경로 또는 상대 경로 + 기본 디렉토리 저장
                fileEntity.setContentType(file.getContentType());
                fileEntity.setFileSize(file.getSize());
                // 스텁 Post 객체를 사용하여 관계 설정 (매퍼 또는 수동 처리)
                fileEntity.setPost(fileMapper.mapPostIdToPostEntity(postId)); // 매퍼 헬퍼 사용

                // 데이터베이스에 엔티티 저장
                savedEntities.add(fileRepository.save(fileEntity));
            }
            // 모든 파일 저장 및 엔티티 지속 성공 시 트랜잭션 커밋됨.
            log.info("게시글 {}에 {}개의 파일 업로드 성공", postId, savedEntities.size());
            return fileMapper.toDto(savedEntities); // 저장된 엔티티를 DTO로 매핑

        } catch (IOException | DataAccessException e) { // 파일 또는 DB 오류 포착
            log.error("게시글 {} 파일 업로드 중 오류 발생. 롤백 시도.", postId, e);
            // 롤백: 실패한 트랜잭션 중에 생성된 파일 삭제
            for (Path path : createdFilePaths) {
                try {
                    java.nio.file.Files.deleteIfExists(path);
                    log.info("파일 생성 롤백됨: {}", path);
                } catch (IOException ex) {
                    log.error("롤백 중 파일 삭제 실패: {}", path, ex);
                    // 삭제 실패 처리 (예: 중요 로그 기록)
                }
            }
            // 트랜잭션 롤백을 보장하기 위해 원본 예외 다시 던지기
            throw e;
        }
    }

    @Transactional // 데이터베이스 및 파일 시스템 변경은 가능하면 원자적이어야 함
    public void deleteFilesByPostId(Long postId) {
        List<Files> filesToDelete = fileRepository.findByPostId(postId);
        if (filesToDelete.isEmpty()) {
            log.info("게시글 ID {}에 대해 삭제할 파일 없음", postId);
            return;
        }

        List<String> failedDeletions = new ArrayList<>();
        for (Files file : filesToDelete) {
            try {
                Path filePath = Paths.get(file.getFilePath());
                if (java.nio.file.Files.deleteIfExists(filePath)) {
                    log.info("디스크에서 파일 삭제됨: {}", filePath);
                } else {
                    log.warn("디스크에서 파일을 찾을 수 없거나 이미 삭제됨: {}", filePath);
                }
            } catch (IOException e) {
                log.error("디스크에서 파일 삭제 실패: {}", file.getFilePath(), e);
                // 전략 결정: 다른 파일 계속 삭제 또는 중지?
                // 실패 수집 후 마지막에 예외 발생 가능.
                failedDeletions.add(file.getFilePath());
            }
        }

        // 파일 삭제 성공/실패 여부와 관계없이 데이터베이스에서 엔티티 삭제?
        // 또는 파일이 성공적으로 삭제된 엔티티만 삭제? 요구사항에 따라 다름.
        // 현재 로직은 게시글 ID와 관련된 모든 DB 항목 삭제.
        fileRepository.deleteAll(filesToDelete);
        log.info("게시글 ID {}에 대한 {}개의 파일 데이터베이스 항목 삭제됨", postId, filesToDelete.size());

        if (!failedDeletions.isEmpty()) {
            // 예외 발생 또는 실패한 파일 삭제 적절히 처리
            log.error("디스크에서 다음 파일들을 삭제할 수 없었습니다: {}", failedDeletions);
            // throw new IOException("게시글 " + postId + "과(와) 관련된 하나 이상의 파일 삭제 실패");
        }
    }

    @Transactional(readOnly = true)
    public FileDTO getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .map(fileMapper::toDto) // 매퍼 사용
                .orElseThrow(() -> new EntityNotFoundException("파일을 찾을 수 없습니다 (ID): " + fileId));
    }

    @Transactional(readOnly = true)
    public List<FileDTO> getFilesByPostId(Long postId) {
        List<Files> files = fileRepository.findByPostId(postId);
        return fileMapper.toDto(files); // 매퍼 사용
    }
}
