package com.glowrise.service;

import com.glowrise.domain.Files;
import com.glowrise.repository.FileRepository;
import com.glowrise.service.dto.FileDTO;
import com.glowrise.service.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FileMapper fileMapper;

    private static final String UPLOAD_DIR = "uploads/";

    // 단일 파일 업로드
    public FileDTO uploadFile(MultipartFile file, Long postId) throws IOException {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        File dest = new File(UPLOAD_DIR + fileName);
        file.transferTo(dest);

        FileDTO fileDTO = new FileDTO();
        fileDTO.setFileName(file.getOriginalFilename());
        fileDTO.setFilePath(UPLOAD_DIR + fileName);
        fileDTO.setContentType(file.getContentType());
        fileDTO.setFileSize(file.getSize());
        fileDTO.setPostId(postId);

        Files fileEntity = fileMapper.toEntity(fileDTO);
        Files savedFile = fileRepository.save(fileEntity);
        return fileMapper.toDto(savedFile);
    }

    // 다중 파일 업로드
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

                FileDTO fileDTO = new FileDTO();
                fileDTO.setFileName(file.getOriginalFilename());
                fileDTO.setFilePath(UPLOAD_DIR + fileName);
                fileDTO.setContentType(file.getContentType());
                fileDTO.setFileSize(file.getSize());
                fileDTO.setPostId(postId);

                Files Files = fileMapper.toEntity(fileDTO);
                Files savedFile = fileRepository.save(Files);
                uploadedFiles.add(fileMapper.toDto(savedFile));
            }
        }
        return uploadedFiles;
    }

    // 파일 삭제
    public void deleteFile(Long fileId) {
        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));

        File fileOnDisk = new File(file.getFilePath());
        if (fileOnDisk.exists()) {
            fileOnDisk.delete();
        }

        fileRepository.delete(file);
    }

    // 게시글에 연관된 파일들 삭제
    public void deleteFilesByPostId(Long postId) {
        List<Files> files = fileRepository.findAll().stream()
                .filter(file -> file.getPost() != null && file.getPost().getId().equals(postId))
                .collect(Collectors.toList());

        for (Files file : files) {
            File fileOnDisk = new File(file.getFilePath());
            if (fileOnDisk.exists()) {
                fileOnDisk.delete();
            }
        }

        fileRepository.deleteAll(files);
    }

    // 파일 조회
    public FileDTO getFileById(Long fileId) {
        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + fileId));
        return fileMapper.toDto(file);
    }

    // 게시글에 연관된 파일 목록 조회
    public List<FileDTO> getFilesByPostId(Long postId) {
        List<Files> files = fileRepository.findAll().stream()
                .filter(file -> file.getPost() != null && file.getPost().getId().equals(postId))
                .collect(Collectors.toList());
        return fileMapper.toDto(files);
    }
}