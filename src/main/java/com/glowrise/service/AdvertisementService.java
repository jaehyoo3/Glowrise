package com.glowrise.service;

import com.glowrise.domain.Advertisement;
import com.glowrise.domain.StoredFile;
import com.glowrise.repository.AdvertisementRepository;
import com.glowrise.repository.FileRepository;
import com.glowrise.service.dto.AdvertisementDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // 생성자 주입 (FileService, Repository 등)
@Transactional
@Slf4j
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    // private final AdvertisementMapper advertisementMapper; // 매퍼 의존성 제거
    private final FileService fileService;
    private final FileRepository fileRepository;

    // --- 수동 변환 헬퍼 메소드 ---

    /**
     * Advertisement 엔티티를 AdvertisementDTO로 수동 변환합니다.
     *
     * @param entity 변환할 Advertisement 엔티티
     * @return 변환된 AdvertisementDTO
     */
    private AdvertisementDTO convertEntityToDto(Advertisement entity) {
        if (entity == null) {
            return null;
        }
        AdvertisementDTO dto = new AdvertisementDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setLinkUrl(entity.getLinkUrl());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setActive(entity.isActive());
        dto.setStartDate(entity.getStartDate()); // LocalDateTime 타입 그대로 전달
        dto.setEndDate(entity.getEndDate());     // LocalDateTime 타입 그대로 전달

        // fileUrl 설정
        StoredFile imageFile = entity.getImageFile();
        if (imageFile != null && imageFile.getId() != null) {
            dto.setFileUrl("/api/files/" + imageFile.getId());
        } else {
            dto.setFileUrl(null);
        }

        return dto;
    }

    /**
     * AdvertisementDTO를 새로운 Advertisement 엔티티로 수동 변환합니다.
     * (주로 광고 생성 시 사용)
     *
     * @param dto 변환할 AdvertisementDTO
     * @return 변환된 Advertisement 엔티티 (ID, imageFile, Auditing 필드는 설정되지 않음)
     */
    private Advertisement convertDtoToEntityForCreate(AdvertisementDTO dto) {
        if (dto == null) {
            return null;
        }
        Advertisement entity = new Advertisement();
        // ID는 자동 생성되므로 설정하지 않음
        entity.setTitle(dto.getTitle());
        entity.setLinkUrl(dto.getLinkUrl());
        entity.setDisplayOrder(dto.getDisplayOrder());
        entity.setActive(dto.isActive());
        entity.setStartDate(dto.getStartDate()); // LocalDateTime 타입 그대로 설정
        entity.setEndDate(dto.getEndDate());     // LocalDateTime 타입 그대로 설정

        // imageFile은 파일 업로드 후 별도로 설정해야 함
        // Auditing 필드는 JPA Auditing 기능이 자동으로 설정함

        return entity;
    }

    // --- 서비스 메소드 (매퍼 호출 대신 수동 변환 사용) ---

    public AdvertisementDTO createAdvertisement(AdvertisementDTO advertisementDTO, MultipartFile imageFile) {
        // DTO -> Entity 수동 변환 (생성용)
        Advertisement advertisement = convertDtoToEntityForCreate(advertisementDTO);
        if (advertisement == null) {
            throw new IllegalArgumentException("Advertisement DTO가 null입니다.");
        }

        // 파일 처리 로직 (기존과 동일)
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                Map<String, Object> uploadResult = fileService.uploadInlineImageAndGetIdUrl(imageFile);
                Long fileId = (Long) uploadResult.get("fileId");
                if (fileId == null) {
                    log.error("FileService.uploadInlineImageAndGetIdUrl 결과에서 fileId를 찾을 수 없습니다.");
                    throw new RuntimeException("광고 이미지 파일 정보 저장에 실패했습니다.");
                }
                StoredFile storedFile = fileRepository.findById(fileId)
                        .orElseThrow(() -> new EntityNotFoundException("저장된 파일 정보를 찾을 수 없습니다 (ID: " + fileId + ")"));
                // 변환된 엔티티에 파일 정보 설정
                advertisement.setImageFile(storedFile);
            } catch (IOException | IllegalArgumentException e) {
                log.error("광고 이미지 파일 저장 중 오류 발생: {}", e.getMessage(), e);
                throw new RuntimeException("광고 이미지 업로드 처리 중 오류가 발생했습니다.", e);
            }
        } else {
            advertisement.setImageFile(null);
        }

        // 엔티티 저장
        advertisement = advertisementRepository.save(advertisement);

        // 저장된 엔티티 -> DTO 수동 변환 후 반환
        return convertEntityToDto(advertisement);
    }


    public Optional<AdvertisementDTO> updateAdvertisement(Long id, AdvertisementDTO advertisementDTO, MultipartFile imageFile) {
        return advertisementRepository.findById(id)
                .map(existingAdvertisement -> { // 기존 엔티티 조회

                    // DTO 값으로 기존 엔티티 필드 직접 업데이트 (수동)
                    existingAdvertisement.setTitle(advertisementDTO.getTitle());
                    existingAdvertisement.setLinkUrl(advertisementDTO.getLinkUrl());
                    existingAdvertisement.setDisplayOrder(advertisementDTO.getDisplayOrder());
                    existingAdvertisement.setActive(advertisementDTO.isActive());
                    existingAdvertisement.setStartDate(advertisementDTO.getStartDate()); // DTO의 LocalDateTime -> Entity의 LocalDateTime
                    existingAdvertisement.setEndDate(advertisementDTO.getEndDate());     // DTO의 LocalDateTime -> Entity의 LocalDateTime

                    StoredFile oldFile = existingAdvertisement.getImageFile();

                    // 파일 처리 로직 (기존과 동일)
                    if (imageFile != null && !imageFile.isEmpty()) {
                        StoredFile newStoredFile = null;
                        try {
                            Map<String, Object> uploadResult = fileService.uploadInlineImageAndGetIdUrl(imageFile);
                            Long newFileId = (Long) uploadResult.get("fileId");
                            if (newFileId == null) {
                                log.error("FileService.uploadInlineImageAndGetIdUrl 결과에서 newFileId를 찾을 수 없습니다.");
                                throw new RuntimeException("새 광고 이미지 파일 정보 저장에 실패했습니다.");
                            }
                            newStoredFile = fileRepository.findById(newFileId)
                                    .orElseThrow(() -> new EntityNotFoundException("저장된 새 파일 정보를 찾을 수 없습니다 (ID: " + newFileId + ")"));
                            // 기존 엔티티에 새 파일 정보 설정
                            existingAdvertisement.setImageFile(newStoredFile);

                            // 기존 파일 삭제
                            if (oldFile != null) {
                                deleteStoredFileInternal(oldFile);
                            }
                        } catch (IOException | IllegalArgumentException e) {
                            log.error("광고 이미지 파일 교체 중 오류 발생: {}", e.getMessage(), e);
                            throw new RuntimeException("광고 이미지 업데이트 처리 중 오류가 발생했습니다.", e);
                        }
                    }

                    // 수정된 엔티티 저장
                    Advertisement updatedAdvertisement = advertisementRepository.save(existingAdvertisement);

                    // 저장된 엔티티 -> DTO 수동 변환 후 반환
                    return convertEntityToDto(updatedAdvertisement);
                });
    }


    @Transactional(readOnly = true)
    public Optional<AdvertisementDTO> getAdvertisement(Long id) {
        return advertisementRepository.findById(id)
                // 매퍼 대신 수동 변환 메소드 사용
                .map(this::convertEntityToDto);
    }


    @Transactional(readOnly = true)
    public List<AdvertisementDTO> getAllActiveAdvertisementsOrdered() {
        List<Advertisement> advertisements = advertisementRepository.findByActiveTrueOrderByDisplayOrderAsc();
        return advertisements.stream()
                // 매퍼 대신 수동 변환 메소드 사용
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<AdvertisementDTO> getAllAdvertisements() {
        return advertisementRepository.findAll().stream()
                // 매퍼 대신 수동 변환 메소드 사용
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }


    // 광고 삭제 (기존 로직 유지)
    public void deleteAdvertisement(Long id) {
        advertisementRepository.findById(id).ifPresent(advertisement -> {
            StoredFile imageFile = advertisement.getImageFile();
            if (imageFile != null) {
                deleteStoredFileInternal(imageFile);
            }
            advertisementRepository.delete(advertisement);
            log.info("광고 삭제 완료 (ID: {})", id);
        });
    }

    // 내부 파일 삭제 메소드 (기존 로직 유지)
    private void deleteStoredFileInternal(StoredFile storedFile) {
        if (storedFile == null || storedFile.getId() == null) {
            log.warn("삭제할 파일 정보가 유효하지 않습니다.");
            return;
        }
        log.info("파일 삭제 시도 (File ID: {})", storedFile.getId());
        Path filePath = Paths.get(storedFile.getFilePath());
        try {
            boolean deletedFromDisk = Files.deleteIfExists(filePath);
            if (deletedFromDisk) {
                log.info("디스크에서 파일 삭제 성공: {}", filePath);
            } else {
                log.warn("디스크 파일을 찾을 수 없거나 이미 삭제됨: {}", filePath);
            }
            if (fileRepository.existsById(storedFile.getId())) {
                fileRepository.delete(storedFile);
                log.info("DB에서 파일 레코드 삭제 성공 (ID: {})", storedFile.getId());
            } else {
                log.warn("DB에서 파일 레코드 (ID: {})를 찾을 수 없어 삭제 스킵됨 (이미 삭제되었을 수 있음)", storedFile.getId());
            }
        } catch (IOException e) {
            log.error("디스크 파일 삭제 실패: {}. DB 레코드 삭제는 시도될 수 있음.", filePath, e);
            try {
                if (fileRepository.existsById(storedFile.getId())) {
                    fileRepository.delete(storedFile);
                    log.warn("디스크 파일 삭제 실패했으나, DB 레코드 삭제 성공 (ID: {})", storedFile.getId());
                }
            } catch (Exception dbEx) {
                log.error("디스크 파일 삭제 실패 후 DB 레코드 삭제 중 추가 오류 발생 (ID: {}): {}", storedFile.getId(), dbEx.getMessage());
            }
        } catch (Exception e) {
            log.error("파일 DB 레코드 삭제 중 예상치 못한 오류 발생 (ID: {}): {}", storedFile.getId(), e.getMessage(), e);
        }
    }
}