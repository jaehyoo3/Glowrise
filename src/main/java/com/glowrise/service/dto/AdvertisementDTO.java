package com.glowrise.service.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdvertisementDTO {
    private Long id;
    private String imageUrl;
    private String targetUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}