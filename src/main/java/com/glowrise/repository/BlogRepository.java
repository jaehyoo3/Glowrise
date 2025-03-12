package com.glowrise.repository;

import com.glowrise.domain.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findByUserId(Long userId);
    boolean existsByUrl(String url);
    Optional<Blog> findByUrl(String url);
}
