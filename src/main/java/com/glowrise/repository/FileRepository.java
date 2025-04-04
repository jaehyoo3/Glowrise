package com.glowrise.repository;

import com.glowrise.domain.Files;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<Files, Long> {
    List<Files> findByPostId(Long postId);
}