package com.glowrise.service;


import com.glowrise.domain.Blog;
import com.glowrise.domain.User;
import com.glowrise.repository.BlogRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.BlogDTO;
import com.glowrise.service.mapper.BlogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogService {
    private final BlogRepository blogRepository;
    private final BlogMapper blogMapper;
    private final UserRepository userRepository;

    public BlogDTO createBlog(BlogDTO dto, Long userId) {
        // 사용자가 이미 블로그를 가지고 있는지 확인
        if (blogRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("해당 사용자는 이미 블로그를 가지고 있습니다.");
        }

        // URL 중복 확인
        if (blogRepository.existsByUrl(dto.getUrl())) {
            throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + dto.getUrl());
        }

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // DTO → 엔티티 변환
        Blog blog = blogMapper.toEntity(dto);
        blog.setUser(user);

        // 저장 및 DTO 반환
        Blog savedBlog = blogRepository.save(blog);
        return blogMapper.toDto(savedBlog);
    }

    // 블로그 수정
    public BlogDTO updateBlog(Long blogId, BlogDTO dto, Long userId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));

        // 권한 확인: 해당 사용자의 블로그인지
        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그를 수정할 권한이 없습니다.");
        }

        // URL 중복 확인 (수정 시 URL이 변경된 경우)
        if (dto.getUrl() != null && !dto.getUrl().equals(blog.getUrl()) && blogRepository.existsByUrl(dto.getUrl())) {
            throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + dto.getUrl());
        }

        // 부분 업데이트
        blogMapper.partialUpdate(blog, dto);

        Blog updatedBlog = blogRepository.save(blog);
        return blogMapper.toDto(updatedBlog);
    }

    // 블로그 제거
    public void deleteBlog(Long blogId, Long userId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new IllegalArgumentException("블로그를 찾을 수 없습니다: " + blogId));

        // 권한 확인
        if (!blog.getUser().getId().equals(userId)) {
            throw new IllegalStateException("해당 블로그를 삭제할 권한이 없습니다.");
        }

        blogRepository.delete(blog);
    }

    // 블로그 목록 조회 (모든 블로그)
    public List<BlogDTO> getAllBlogs() {
        List<Blog> blogs = blogRepository.findAll();
        return blogMapper.toDto(blogs);
    }

    // 특정 사용자의 블로그 조회
    public BlogDTO getBlogByUserId(Long userId) {
        Blog blog = blogRepository.findByUserId(userId)
                .orElse(null); // 블로그가 없을 수 있음
        return blog != null ? blogMapper.toDto(blog) : null;
    }

    // URL 중복 확인
    public boolean isUrlAvailable(String url) {
        return !blogRepository.existsByUrl(url);
    }
}