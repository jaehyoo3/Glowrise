package com.glowrise.service;

import com.glowrise.domain.Advertisement;
import com.glowrise.repository.AdvertisementRepository;
import com.glowrise.service.dto.AdvertisementDTO;
import com.glowrise.service.mapper.AdvertisementMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AdvertisementService {

    private final Logger log = LoggerFactory.getLogger(AdvertisementService.class);
    private final AdvertisementRepository advertisementRepository;
    private final AdvertisementMapper advertisementMapper;

    @Transactional(readOnly = true)
    public List<AdvertisementDTO> getActiveAdvertisements() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Finding active advertisements for time: {}", now);
        List<Advertisement> activeAds = advertisementRepository.findActiveAdvertisements(now);

        return activeAds.stream()
                .limit(6)
                .map(advertisementMapper::toDto)
                .collect(Collectors.toList());
    }

    public AdvertisementDTO createAdvertisement(AdvertisementDTO advertisementDTO) {
        log.debug("Request to save Advertisement : {}", advertisementDTO);
        Advertisement advertisement = advertisementMapper.toEntity(advertisementDTO);
        advertisement = advertisementRepository.save(advertisement);
        return advertisementMapper.toDto(advertisement);
    }

    @Transactional(readOnly = true)
    public List<AdvertisementDTO> getAllAdvertisements() {
        log.debug("Request to get all Advertisements");
        return advertisementRepository.findAll().stream()
                .map(advertisementMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdvertisementDTO getAdvertisementById(Long id) {
        log.debug("Request to get Advertisement : {}", id);
        return advertisementRepository.findById(id)
                .map(advertisementMapper::toDto)
                .orElse(null); // 또는 Optional<AdvertisementDTO> 반환
    }

    public AdvertisementDTO updateAdvertisement(AdvertisementDTO advertisementDTO) {
        log.debug("Request to update Advertisement : {}", advertisementDTO);
        return advertisementRepository.findById(advertisementDTO.getId()).map(existingAd -> {
            advertisementMapper.partialUpdate(existingAd, advertisementDTO);
            existingAd.setImageUrl(advertisementDTO.getImageUrl());
            existingAd.setTargetUrl(advertisementDTO.getTargetUrl());
            existingAd.setStartDate(advertisementDTO.getStartDate());
            existingAd.setEndDate(advertisementDTO.getEndDate());
            existingAd.setDisplayOrder(advertisementDTO.getDisplayOrder());
            existingAd.setIsActive(advertisementDTO.getIsActive());
            return advertisementMapper.toDto(existingAd);
        }).orElseThrow(() -> new RuntimeException("Advertisement not found with id " + advertisementDTO.getId()));
    }

    public void deleteAdvertisement(Long id) {
        log.debug("Request to delete Advertisement : {}", id);
        advertisementRepository.deleteById(id);
    }
}