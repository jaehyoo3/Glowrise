package com.glowrise.repository;

import com.glowrise.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByMenuId(Long menuId);
    List<Post> findByAuthorId(Long userId);
}
