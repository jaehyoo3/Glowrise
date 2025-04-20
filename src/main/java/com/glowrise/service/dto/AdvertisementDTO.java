package com.glowrise.service.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class AdvertisementDTO {
    private Long id;
    private String title;
    private String linkUrl;
    private int displayOrder;
    private boolean active;
    private String createdBy;
    private LocalDateTime createdDate;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedDate;
    private String fileUrl;
    private Long storedFileId;

    // 시작일시 필드 (타입 확인 및 어노테이션 추가)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    // 종료일시 필드 (타입 확인 및 어노테이션 추가)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;
}