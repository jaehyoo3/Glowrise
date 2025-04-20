package com.glowrise.web;

import com.glowrise.service.AdvertisementService;
import com.glowrise.service.dto.AdvertisementDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/advertisements")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService advertisementService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    public ResponseEntity<AdvertisementDTO> createAdvertisement(
            @RequestPart("advertisement") AdvertisementDTO advertisementDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) throws URISyntaxException {

        if (imageFile == null || imageFile.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        AdvertisementDTO result = advertisementService.createAdvertisement(advertisementDTO, imageFile);
        return ResponseEntity.created(new URI("/api/advertisements/" + result.getId()))
                .body(result);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // consumes 타입 변경
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    public ResponseEntity<AdvertisementDTO> updateAdvertisement(
            @PathVariable Long id,
            @RequestPart("advertisement") AdvertisementDTO advertisementDTO,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        return advertisementService.updateAdvertisement(id, advertisementDTO, imageFile)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    public ResponseEntity<List<AdvertisementDTO>> getAllAdvertisements() {
        List<AdvertisementDTO> list = advertisementService.getAllAdvertisements(); // 관리용 전체 목록 조회 메소드 사용
        return ResponseEntity.ok(list);
    }

    @GetMapping("/active")
    public ResponseEntity<List<AdvertisementDTO>> getActiveAdvertisements() {
        List<AdvertisementDTO> list = advertisementService.getAllActiveAdvertisementsOrdered();
        log.info("dd {}", list);
        return ResponseEntity.ok(list);
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 관리자만 상세 조회 가능하도록 (필요시 변경)
    public ResponseEntity<AdvertisementDTO> getAdvertisement(@PathVariable Long id) {
        return advertisementService.getAdvertisement(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    public ResponseEntity<Void> deleteAdvertisement(@PathVariable Long id) {
        advertisementService.deleteAdvertisement(id);
        return ResponseEntity.noContent().build();
    }
}