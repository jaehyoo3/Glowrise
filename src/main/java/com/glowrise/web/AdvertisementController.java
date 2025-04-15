package com.glowrise.web;

import com.glowrise.service.AdvertisementService;
import com.glowrise.service.dto.AdvertisementDTO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdvertisementController {

    private final Logger log = LoggerFactory.getLogger(AdvertisementController.class);
    private final AdvertisementService advertisementService;

    @GetMapping("/advertisements/active")
    public ResponseEntity<List<AdvertisementDTO>> getActiveAdvertisements() {
        log.debug("REST request to get active Advertisements");
        List<AdvertisementDTO> activeAds = advertisementService.getActiveAdvertisements();
        return ResponseEntity.ok(activeAds);
    }

    @PostMapping("/admin/advertisements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdvertisementDTO> createAdvertisement(@RequestBody AdvertisementDTO advertisementDTO) throws URISyntaxException {
        log.debug("REST request to save Advertisement : {}", advertisementDTO);
        if (advertisementDTO.getId() != null) {
            return ResponseEntity.badRequest().body(null);
        }
        AdvertisementDTO result = advertisementService.createAdvertisement(advertisementDTO);
        return ResponseEntity.created(new URI("/api/admin/advertisements/" + result.getId()))
                .body(result);
    }


    @GetMapping("/admin/advertisements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdvertisementDTO>> getAllAdvertisements() {
        log.debug("REST request to get all Advertisements");
        List<AdvertisementDTO> list = advertisementService.getAllAdvertisements();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/admin/advertisements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdvertisementDTO> getAdvertisement(@PathVariable Long id) {
        log.debug("REST request to get Advertisement : {}", id);
        AdvertisementDTO advertisementDTO = advertisementService.getAdvertisementById(id);
        return advertisementDTO != null ? ResponseEntity.ok(advertisementDTO) : ResponseEntity.notFound().build();
    }


    @PutMapping("/admin/advertisements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdvertisementDTO> updateAdvertisement(@PathVariable Long id, @RequestBody AdvertisementDTO advertisementDTO) {
        log.debug("REST request to update Advertisement : {}, {}", id, advertisementDTO);
        if (advertisementDTO.getId() == null || !id.equals(advertisementDTO.getId())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            AdvertisementDTO result = advertisementService.updateAdvertisement(advertisementDTO);
            return ResponseEntity.ok().body(result);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/admin/advertisements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAdvertisement(@PathVariable Long id) {
        log.debug("REST request to delete Advertisement : {}", id);
        advertisementService.deleteAdvertisement(id);
        return ResponseEntity.noContent().build();
    }
}