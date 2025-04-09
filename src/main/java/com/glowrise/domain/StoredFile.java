package com.glowrise.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "files")
@Table
@Getter
@Setter
public class StoredFile extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName; // 원본 파일 이름

    @Column(nullable = false)
    private String filePath; // 서버에 저장된 파일 경로

    @Column(nullable = false)
    private String contentType; // 파일 MIME 타입 (예: "image/png")

    @Column(nullable = false)
    private Long fileSize; // 파일 크기 (바이트 단위)

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post; // 파일이 속한 게시글 (nullable)
}