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

    // --- 공개 API 메소드 ---

    @Transactional
    public BlogDTO createBlog(BlogDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 인증된 사용자 ID 가져오기
        User user = findUserByIdOrThrow(userId); // 사용자 조회 또는 예외 발생

        // 사용자가 이미 블로그를 가지고 있는지 확인
        if (blogRepository.existsByUserId(userId)) {
            throw new IllegalStateException("사용자는 이미 블로그를 가지고 있습니다.");
        }

        // 진행하기 전에 URL 사용 가능 여부 확인
        validateUrlAvailability(dto.getUrl(), null); // 새 블로그이므로 existingBlogUrl은 null

        Blog blog = new Blog(dto); // Blog 생성자가 DTO로부터 관련 필드를 받는다고 가정
        blog.setUser(user);
        // 만약 User 엔티티가 관계를 관리한다면, 일관성 보장:
        // user.setBlog(blog); // User가 Blog에 대한 @OneToOne 매핑을 가지고 있다면 주석 해제

        Blog savedBlog = blogRepository.save(blog);
        log.info("사용자 ID {}에 대한 블로그 생성 성공", userId);
        return blogMapper.toDto(savedBlog);
    }

    @Transactional
    public BlogDTO updateBlog(Long blogId, BlogDTO dto, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 인증된 사용자 ID 가져오기
        Blog existingBlog = findBlogByIdOrThrow(blogId); // 블로그 조회 또는 예외 발생

        // 소유권 확인
        ensureBlogOwnership(existingBlog, userId);

        // URL 사용 가능 여부 확인 (URL이 제공되고 변경된 경우에만)
        if (dto.getUrl() != null && !dto.getUrl().equals(existingBlog.getUrl())) {
            validateUrlAvailability(dto.getUrl(), existingBlog.getUrl());
        }

        // 매퍼를 사용한 부분 업데이트
        blogMapper.partialUpdate(existingBlog, dto);

        Blog updatedBlog = blogRepository.save(existingBlog); // JPA가 업데이트 관리
        log.info("블로그 업데이트 성공: {}", blogId);
        return blogMapper.toDto(updatedBlog);
    }

    @Transactional(readOnly = true) // 읽기 작업에 좋은 관행
    public BlogDTO getMyBlog(Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 인증된 사용자 ID 가져오기
        return blogRepository.findByUserId(userId)
                .map(blogMapper::toDto)
                .orElse(null); // 또는 사용자가 로그인 후 반드시 블로그를 가져야 한다면 NotFoundException 발생
    }

    // 원본 코드의 주석에 따라 인증 확인 제거됨
    @Transactional(readOnly = true)
    public BlogDTO getBlogById(Long id) {
        return blogRepository.findById(id)
                .map(blogMapper::toDto)
                .orElse(null); // 일관성을 위해 여기서 EntityNotFoundException 발생 고려
    }

    @Transactional // 격리가 특별히 필요한 경우가 아니면 REQUIRES_NEW 제거
    public void deleteBlog(Long blogId, Authentication authentication) {
        Long userId = getAuthenticatedUserId(authentication); // 인증된 사용자 ID 가져오기
        Blog blog = findBlogByIdOrThrow(blogId); // 블로그 조회 또는 예외 발생

        // 소유권 확인
        ensureBlogOwnership(blog, userId);

        // 필요한 경우 User 측에서 관계 정리 (매핑에 따라 다름)
        User user = blog.getUser();
        if (user != null) {
            user.setBlog(null);
            // userRepository.save(user); // User 엔티티가 관계를 소유하고 cascade가 설정되지 않은 경우에만
        }

        blogRepository.deleteById(blogId);
        log.info("블로그 삭제 성공: {}", blogId);
        // log.info("삭제 후 - 존재 여부: {}", blogRepository.existsById(blogId)); // 디버깅용
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
                    // 매퍼가 userId를 올바르게 매핑하는지 확인하거나 필요한 경우 여기서 설정
                    if (blog.getUser() != null) {
                        dto.setUserId(blog.getUser().getId());
                    }
                    return dto;
                })
                .orElse(null); // EntityNotFoundException 발생 고려
    }


    // --- 비공개 헬퍼 메소드 ---

    /**
     * Authentication 객체로부터 사용자 ID를 검색합니다.
     * 인증되지 않은 경우 IllegalStateException을 발생시킵니다.
     */
    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증이 필요합니다."); // 또는 AuthenticationCredentialsNotFoundException
        }
        String username = authentication.getName();
        // 최적화: Principal의 UserDetails에 ID가 포함되어 있다면 직접 사용합니다.
        // 그렇지 않으면 사용자를 조회합니다.
        return findUserByUsernameOrThrow(username).getId();
    }

    /**
     * 사용자 이름으로 User를 찾거나 EntityNotFoundException을 발생시킵니다.
     */
    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다 (사용자 이름): " + username));
    }

    /**
     * ID로 User를 찾거나 EntityNotFoundException을 발생시킵니다.
     */
    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다 (ID): " + userId));
    }

    /**
     * ID로 Blog를 찾거나 EntityNotFoundException을 발생시킵니다.
     */
    private Blog findBlogByIdOrThrow(Long blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("블로그를 찾을 수 없습니다 (ID): " + blogId));
    }

    /**
     * 인증된 사용자가 블로그를 소유하고 있는지 확인합니다. 그렇지 않으면 AccessDeniedException을 발생시킵니다.
     */
    private void ensureBlogOwnership(Blog blog, Long userId) {
        if (blog.getUser() == null || !blog.getUser().getId().equals(userId)) {
            log.warn("사용자 {}가 블로그 {} 수정을 시도하여 접근 거부됨", userId, blog.getId());
            throw new AccessDeniedException("이 블로그를 수정할 권한이 없습니다.");
        }
    }

    /**
     * URL이 사용 가능한지 확인하고, 그렇지 않으면 IllegalArgumentException을 발생시킵니다.
     */
    private void validateUrlAvailability(String newUrl, String existingBlogUrl) {
        // URL이 null이 아니고 (새 블로그이거나 변경된 경우) 에만 확인
        if (newUrl != null && (existingBlogUrl == null || !newUrl.equals(existingBlogUrl))) {
            if (blogRepository.existsByUrl(newUrl)) {
                throw new IllegalArgumentException("이미 사용 중인 URL입니다: " + newUrl);
            }
        }
    }
}