package com.glowrise.service;

import com.glowrise.domain.Blog;
import com.glowrise.domain.User;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.mapper.BlogMapper;
import com.glowrise.service.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository; // Keep for user.setBlog(null)

    @Transactional
    public BlogDTO createBlog(BlogDTO dto, Authentication ignoredAuthentication) {
        User user = securityUtil.getCurrentUserOrThrow();
        Long userId = user.getId();

        if (blogRepository.existsByUserId(userId)) {
            throw new IllegalStateException("사용자는 이미 블로그를 가지고 있습니다.");
        }

        validateUrlAvailability(dto.getUrl(), null);

        Blog blog = new Blog(dto);
        blog.setUser(user);
        user.setBlog(blog);

        Blog savedBlog = blogRepository.save(blog);
        return blogMapper.toDto(savedBlog);
    }

    @Transactional
    @PreAuthorize("@authorizationService.isBlogOwner(#blogId)")
    public BlogDTO updateBlog(Long blogId, BlogDTO dto, Authentication ignoredAuthentication) {
        Blog existingBlog = findBlogByIdOrThrow(blogId);

        if (dto.getUrl() != null && !dto.getUrl().equals(existingBlog.getUrl())) {
            validateUrlAvailability(dto.getUrl(), existingBlog.getUrl());
        }

        blogMapper.partialUpdate(existingBlog, dto);

        Blog updatedBlog = blogRepository.save(existingBlog);
        return blogMapper.toDto(updatedBlog);
    }

    @Transactional(readOnly = true)
    public BlogDTO getMyBlog(Authentication ignoredAuthentication) {
        Long userId = securityUtil.getCurrentUserIdOrThrow();
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
    @PreAuthorize("@authorizationService.isBlogOwner(#blogId)")
    public void deleteBlog(Long blogId, Authentication ignoredAuthentication) {
        Blog blog = findBlogByIdOrThrow(blogId);

        User user = blog.getUser();
        if (user != null) {
            user.setBlog(null);
            userRepository.save(user);
        }

        blogRepository.deleteById(blogId);
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

    private Blog findBlogByIdOrThrow(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("블로그를 찾을 수 없습니다 (ID): " + blogId));
    }

    private void validateUrlAvailability(String newUrl, String existingBlogUrl) {
        if (newUrl != null && (!newUrl.equals(existingBlogUrl))) {
            if (blogRepository.existsByUrl(newUrl)) {
                throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + newUrl);
            }
        }
    }
}