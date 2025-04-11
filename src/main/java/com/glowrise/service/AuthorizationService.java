package com.glowrise.service;

import com.glowrise.domain.Blog;
import com.glowrise.domain.Comment;
import com.glowrise.domain.Post;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.CommentRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.service.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationService {

    private final SecurityUtil securityUtil;
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public boolean isBlogOwner(Long blogId) {
        Long currentUserId = securityUtil.getCurrentUserId().orElse(null);
        if (currentUserId == null) return false;

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("블로그를 찾을 수 없습니다 (ID: " + blogId + ")"));

        return blog.getUser() != null && Objects.equals(blog.getUser().getId(), currentUserId);
    }

    public boolean isBlogOwnerByMenuId(Long menuId) {
        Long currentUserId = securityUtil.getCurrentUserId().orElse(null);
        if (currentUserId == null) return false;

        Blog blog = blogRepository.findByMenusId(menuId) // MenuRepository에 findBlogByMenuId 같은 메소드나 BlogRepo에 연관쿼리 필요
                .orElseThrow(() -> new EntityNotFoundException("메뉴에 해당하는 블로그를 찾을 수 없습니다 (Menu ID: " + menuId + ")"));

        return blog.getUser() != null && Objects.equals(blog.getUser().getId(), currentUserId);
    }


    public boolean isPostOwner(Long postId) {
        Long currentUserId = securityUtil.getCurrentUserId().orElse(null);
        if (currentUserId == null) return false;

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다 (ID: " + postId + ")"));

        return post.getAuthor() != null && Objects.equals(post.getAuthor().getId(), currentUserId);
    }

    public boolean isCommentOwner(Long commentId) {
        Long currentUserId = securityUtil.getCurrentUserId().orElse(null);
        if (currentUserId == null) return false;

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다 (ID: " + commentId + ")"));

        return comment.getUser() != null && Objects.equals(comment.getUser().getId(), currentUserId);
    }
}