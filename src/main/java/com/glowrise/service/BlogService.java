package com.glowrise.service;

import com.glowrise.domain.Blog;
import com.glowrise.domain.User;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.mapper.BlogMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
        Long userId = getAuthenticatedUserId(authentication);
        User user = findUserByIdOrThrow(userId);

        if (blogRepository.existsByUserId(userId)) {
            throw new IllegalStateException("사용자는 이미 블로그를 가지고 있습니다.");
        }

        validateUrlAvailability(dto.getUrl(), null);

        Blog blog = new Blog(dto);
        blog.setUser(user);
        user.setBlog(blog);

        Blog savedBlog = blogRepository.save(blog);
        log.info("사용자 ID {}에 대한 블로그 생성 성공", userId);
        return blogMapper.toDto(savedBlog);
    }

    @Transactional
    public BlogDTO updateBlog(Long blogId, BlogDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Blog existingBlog = findBlogByIdOrThrow(blogId);

        ensureBlogOwnership(existingBlog, userId);

        if (dto.getUrl() != null && !dto.getUrl().equals(existingBlog.getUrl())) {
            validateUrlAvailability(dto.getUrl(), existingBlog.getUrl());
        }

        blogMapper.partialUpdate(existingBlog, dto);

        Blog updatedBlog = blogRepository.save(existingBlog);
        log.info("블로그 업데이트 성공: {}", blogId);
        return blogMapper.toDto(updatedBlog);
    }

    @Transactional(readOnly = true)
    public BlogDTO getMyBlog(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        return blogRepository.findByUserId(userId)
                .map(blogMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public BlogDTO getBlogById(Long id) {
        return blogRepository.findById(id)
                .map(blogMapper::toDto)
                .orElse(null);
    }

    @Transactional
    public void deleteBlog(Long blogId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication);
        Blog blog = findBlogByIdOrThrow(blogId);

        ensureBlogOwnership(blog, userId);

        User user = blog.getUser();
        if (user != null) {
            user.setBlog(null);
            userRepository.save(user);
        }

        blogRepository.deleteById(blogId);
        log.info("블로그 삭제 성공: {}", blogId);
    }

    @Transactional(readOnly = true)
    public List<BlogDTO> getAllBlogs() {
        return blogMapper.toDto(blogRepository.findAll());
    }

    @Transactional(readOnly = true)
    public BlogDTO getBlogByUserId(Long userId) {
        return blogRepository.findByUserId(userId)
                .map(blogMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isUrlAvailable(String url) {
        return !blogRepository.existsByUrl(url);
    }

    @Transactional(readOnly = true)
    public BlogDTO getBlogByUrl(String url) {
        return blogRepository.findByUrl(url)
                .map(blog -> {
                    BlogDTO dto = blogMapper.toDto(blog);
                    if (blog.getUser() != null) {
                        dto.setUserId(blog.getUser().getId());
                    }
                    return dto;
                })
                .orElse(null);
    }


    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증이 필요합니다.");
        }
        String username = authentication.getName();
        return findUserByUsernameOrThrow(username).getId();
    }

    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다 (사용자 이름): " + username));
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다 (ID): " + userId));
    }

    private Blog findBlogByIdOrThrow(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("블로그를 찾을 수 없습니다 (ID): " + blogId));
    }

    private void ensureBlogOwnership(Blog blog, Long userId) {
        if (blog.getUser() == null || !blog.getUser().getId().equals(userId)) {
            log.warn("사용자 {}가 블로그 {} 수정을 시도하여 접근 거부됨", userId, blog.getId());
            throw new AccessDeniedException("이 블로그를 수정할 권한이 없습니다.");
        }
    }

    private void validateUrlAvailability(String newUrl, String existingBlogUrl) {
        if (newUrl != null && (existingBlogUrl == null || !newUrl.equals(existingBlogUrl))) {
            if (blogRepository.existsByUrl(newUrl)) {
                throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + newUrl);
            }
        }
    }
}