package com.glowrise.repository;

import com.glowrise.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByParentId(Long parentId);

    List<Comment> findByPostIdAndParentIsNull(Long postId);
}

