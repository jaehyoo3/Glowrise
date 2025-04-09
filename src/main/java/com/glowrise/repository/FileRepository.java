package com.glowrise.repository;

import com.glowrise.domain.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findByPostId(Long postId);

    List<StoredFile> findByPostIsNullAndCreatedDateBefore(LocalDateTime threshold); // 추가

}