package com.glowrise.service;

import com.glowrise.domain.Files;
import com.glowrise.domain.Post;
import com.glowrise.repository.FileRepository;
import com.glowrise.service.dto.FileDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;

    private static final String UPLOAD_DIR = "D:/uploads/";

    public List<FileDTO> uploadFiles(List<MultipartFile> files, Long postId) throws IOException {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<FileDTO> uploadedFiles = new ArrayList<>();
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                File dest = new File(UPLOAD_DIR + fileName);
                file.transferTo(dest);

                // FileDTO 생성
                FileDTO fileDTO = new FileDTO();
                fileDTO.setFileName(file.getOriginalFilename());
                fileDTO.setFilePath(UPLOAD_DIR + fileName);
                fileDTO.setContentType(file.getContentType());
                fileDTO.setFileSize(file.getSize());
                fileDTO.setPostId(postId);

                // Files 엔티티로 변환 (수동 매핑)
                Files fileEntity = new Files();
                fileEntity.setFileName(fileDTO.getFileName());
                fileEntity.setFilePath(fileDTO.getFilePath());
                fileEntity.setContentType(fileDTO.getContentType());
                fileEntity.setFileSize(fileDTO.getFileSize());

                // Post 객체 설정
                Post post = new Post();
                post.setId(postId);
                fileEntity.setPost(post);

                // 파일 저장
                Files savedFile = fileRepository.save(fileEntity);

                // 저장된 Files를 FileDTO로 변환 (수동 매핑)
                FileDTO savedFileDTO = new FileDTO();
                savedFileDTO.setId(savedFile.getId());
                savedFileDTO.setFileName(savedFile.getFileName());
                savedFileDTO.setFilePath(savedFile.getFilePath());
                savedFileDTO.setContentType(savedFile.getContentType());
                savedFileDTO.setFileSize(savedFile.getFileSize());
                savedFileDTO.setPostId(savedFile.getPost() != null ? savedFile.getPost().getId() : null);

                uploadedFiles.add(savedFileDTO);
            }
        }
        return uploadedFiles;
    }

    // 기타 메서드 (deleteFilesByPostId 등)에서 MapStruct 사용 부분도 수동으로 변경 필요
    public void deleteFilesByPostId(Long postId) {
        List<Files> files = fileRepository.findByPostId(postId);
        for (Files file : files) {
            File fileOnDisk = new File(file.getFilePath());
            if (fileOnDisk.exists()) {
                fileOnDisk.delete();
            }
        }
        fileRepository.deleteAll(files);
    }

    public FileDTO getFileById(Long fileId) {
        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));

        // Files를 FileDTO로 변환 (수동 매핑)
        FileDTO fileDTO = new FileDTO();
        fileDTO.setId(file.getId());
        fileDTO.setFileName(file.getFileName());
        fileDTO.setFilePath(file.getFilePath());
        fileDTO.setContentType(file.getContentType());
        fileDTO.setFileSize(file.getFileSize());
        fileDTO.setPostId(file.getPost() != null ? file.getPost().getId() : null);

        return fileDTO;
    }

    public List<FileDTO> getFilesByPostId(Long postId) {
        List<Files> files = fileRepository.findByPostId(postId);

        // Files 리스트를 FileDTO 리스트로 변환 (수동 매핑)
        List<FileDTO> fileDTOs = new ArrayList<>();
        for (Files file : files) {
            FileDTO fileDTO = new FileDTO();
            fileDTO.setId(file.getId());
            fileDTO.setFileName(file.getFileName());
            fileDTO.setFilePath(file.getFilePath());
            fileDTO.setContentType(file.getContentType());
            fileDTO.setFileSize(file.getFileSize());
            fileDTO.setPostId(file.getPost() != null ? file.getPost().getId() : null);
            fileDTOs.add(fileDTO);
        }

        return fileDTOs;
    }
}