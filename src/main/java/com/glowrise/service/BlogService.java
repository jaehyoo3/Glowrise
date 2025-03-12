package com.glowrise.service;


import com.glowrise.config.jwt.dto.CustomOAuthUser;
import com.glowrise.domain.Blog;
import com.glowrise.domain.User;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.mapper.BlogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlogService {
    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
    private final UserRepository userRepository;

    @Transactional
    public BlogDTO createBlog(BlogDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication2(authentication);

        // 중복 블로그 체크
        if (blogRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("해당 사용자는 이미 블로그를 가지고 있습니다.");
        }

        // URL 중복 체크
        if (blogRepository.existsByUrl(dto.getUrl())) {
            throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + dto.getUrl());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        Blog blog = blogMapper.toEntity(dto);
        blog.setId(null);  // 명시적으로 ID를 null로 설정
        blog.setUser(user);
        Blog savedBlog = blogRepository.save(blog);

        return blogMapper.toDto(savedBlog);
    }

    private Long getUserIdFromAuthentication2(Authentication authentication) {
        System.out.println("Authentication: " + authentication.getName());
        if (authentication == null) {
            throw new IllegalStateException("Authentication 객체가 null입니다.");
        }
        Object principal = authentication.getPrincipal();
        System.out.println("Principal: " + principal + ", Type: " + (principal != null ? principal.getClass().getName() : "null"));

        if (principal instanceof CustomOAuthUser user) {
            Long userId = user.getUserId();
            System.out.println("CustomOAuthUser detected, userId: " + userId);
            return userId;
        }
        throw new IllegalStateException("인증된 사용자의 ID를 가져올 수 없습니다. Principal 타입: " + principal.getClass().getName());
    }

    public BlogDTO updateBlog(Long blogId, BlogDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));
        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그를 수정할 권한이 없습니다.");
        }
        if (dto.getUrl() != null && !dto.getUrl().equals(blog.getUrl()) && blogRepository.existsByUrl(dto.getUrl())) {
            throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + dto.getUrl());
        }
        blogMapper.partialUpdate(blog, dto);
        Blog updatedBlog = blogRepository.save(blog);
        return blogMapper.toDto(updatedBlog);
    }

    public void deleteBlog(Long blogId, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));
        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그를 삭제할 권한이 없습니다.");
        }
        blogRepository.delete(blog);
    }

    public List<BlogDTO> getAllBlogs() {
        List<Blog> blogs = blogRepository.findAll();
        return blogMapper.toDto(blogs);
    }

    public BlogDTO getBlogByUserId(Long userId) {
        Blog blog = blogRepository.findByUserId(userId).orElse(null);
        return blog != null ? blogMapper.toDto(blog) : null;
    }

    public boolean isUrlAvailable(String url) {
        return !blogRepository.existsByUrl(url);
    }

    public BlogDTO getBlogByUrl(String url) {
        Blog blog = blogRepository.findByUrl(url)
                .orElse(null);
        return blog != null ? blogMapper.toDto(blog) : null;
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        return user.getId();
    }
}