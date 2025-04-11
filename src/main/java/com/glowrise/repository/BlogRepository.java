package com.glowrise.repository;

import com.glowrise.domain.Blog;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findByUserId(Long userId);
    boolean existsByUrl(String url);
    Optional<Blog> findByUrl(String url);

    @Query("SELECT m.blog FROM Menu m WHERE m.id = :menuId")
    Optional<Blog> findByMenusId(@Param("menuId") Long menuId);
    boolean existsByUserId(Long userId);
}
