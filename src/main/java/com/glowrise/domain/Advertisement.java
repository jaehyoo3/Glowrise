package com.glowrise.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "advertisement")
@Getter
@Setter
public class Advertisement extends AbstractAuditingEntity<Long> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "link_url", length = 1024)
    private String linkUrl;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true; // 광고 활성 상태

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "stored_file_id", referencedColumnName = "id")
    private StoredFile imageFile; // 광고 이미지 파일

    @Column(name = "start_date", nullable = false) // DB 컬럼명 'start_date', NOT NULL 가정
    private LocalDateTime startDate; // 타입: LocalDateTime

    @Column(name = "end_date", nullable = false)   // DB 컬럼명 'end_date', NOT NULL 가정
    private LocalDateTime endDate;   // 타입: LocalDateTime

}