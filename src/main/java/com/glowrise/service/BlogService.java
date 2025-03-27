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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
@Service
@Slf4j
@RequiredArgsConstructor
public class BlogService {
    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
    private final UserRepository userRepository;

    public BlogDTO createBlog(BlogDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);

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

        Blog blog = new Blog(dto);
        blog.setUser(user);

        Blog savedBlog = blogRepository.save(blog);
        return blogMapper.toDto(savedBlog);
    }

    @Transactional
    public Optional<BlogDTO> updateBlog(Long blogId, BlogDTO dto, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);

        return blogRepository.findById(blogId)
                .map(existingBlog -> {
                    // 권한 체크
                    if (!existingBlog.getUser().getId().equals(userId)) {
                        throw new IllegalStateException("해당 블로그를 수정할 권한이 없습니다.");
                    }
                    // URL 중복 체크
                    if (dto.getUrl() != null && !dto.getUrl().equals(existingBlog.getUrl()) && blogRepository.existsByUrl(dto.getUrl())) {
                        throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + dto.getUrl());
                    }
                    // 부분 업데이트
                    blogMapper.partialUpdate(existingBlog, dto);
                    return existingBlog;
                })
                .map(blogRepository::save)
                .map(blogMapper::toDto);
    }

    public BlogDTO getMyBlog(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findByUserId(userId).orElse(null);
        return blog != null ? blogMapper.toDto(blog) : null;
    }

    // 조회 권한 체크 제거
    public BlogDTO getBlogById(Long id) {
        Blog blog = blogRepository.findById(id).orElse(null);
        if (blog == null) {
            log.info("Blog not found for ID: {}", id);
            return null;
        }
        return blogMapper.toDto(blog);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteBlog(Long blogId, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));
        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그를 삭제할 권한이 없습니다.");
        }
        User user = blog.getUser();
        user.setBlog(null);

        blogRepository.deleteById(blogId);
        log.info("After delete - exists: {}", blogRepository.existsById(blogId));
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
        Blog blog = blogRepository.findByUrl(url).orElse(null);
        BlogDTO blogDTO = blogMapper.toDto(blog);
        blogDTO.setUserId(blog.getUser().getId());
        return blogDTO;
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        return user.getId();
    }
}