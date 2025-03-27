package com.glowrise.web;

import com.glowrise.service.PostService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.PostDTO;
import com.glowrise.service.dto.PostListDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PostDTO> createPost(
            @RequestPart("dto") PostDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {
        checkAuthentication(authentication);
        System.out.println("Received POST /api/posts, DTO: " + dto);
        try {
            PostDTO createdPost = postService.createPost(dto, files, authentication);
            return ResponseEntity.ok(createdPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping(value = "/{postId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable Long postId,
            @RequestPart("dto") PostDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {
        checkAuthentication(authentication);
        PostDTO updatedPost = postService.updatePost(postId, dto, files, authentication);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @RequestParam Long userId,
            Authentication authentication) {
        checkAuthentication(authentication);
        postService.deletePost(postId, userId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        List<PostDTO> posts = postService.getAllPosts();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUserId(@PathVariable Long userId) {
        List<PostDTO> posts = postService.getPostsByUserId(userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPostById(
            @PathVariable Long postId,
            HttpServletRequest request,
            Authentication authentication) {
        String clientIp = request.getRemoteAddr(); // 기본 IP 추출
        // 프록시 환경에서는 X-Forwarded-For 헤더를 고려해야 할 수 있음
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            clientIp = forwardedFor.split(",")[0].trim(); // 첫 번째 IP 사용
        }

        PostDTO post = postService.getPostById(postId, clientIp, authentication);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/blog/{blogId}/{menuId}")
    public ResponseEntity<Page<PostDTO>> getPostsByBlogIdAndMenuId(
            @PathVariable Long blogId,
            @PathVariable Long menuId,
            @RequestParam(required = false) String searchKeyword,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostDTO> posts = postService.getPosts(blogId, menuId, searchKeyword, pageable);
        return ResponseEntity.ok(posts);
    }

    // menuId가 없는 경우: 전체 게시글 조회
    @GetMapping("/blog/{blogId}")
    public ResponseEntity<Page<PostDTO>> getPostsByBlogId(
            @PathVariable Long blogId,
            @RequestParam(required = false) String searchKeyword,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostDTO> posts = postService.getPosts(blogId, null, searchKeyword, pageable);
        return ResponseEntity.ok(posts);
    }

    private void checkAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}