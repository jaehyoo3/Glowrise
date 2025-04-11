package com.glowrise.web;

import com.glowrise.domain.enumerate.TimePeriod;
import com.glowrise.service.PostService;
import com.glowrise.service.dto.PostDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostDTO> createPost(
            @RequestPart("dto") PostDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {
        PostDTO createdPost = postService.createPost(dto, files, authentication);
        return ResponseEntity.ok(createdPost);
    }

    @PutMapping(value = "/{postId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable Long postId,
            @RequestPart("dto") PostDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {
        PostDTO updatedPost = postService.updatePost(postId, dto, files, authentication);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{postId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            Authentication authentication) {
        postService.deletePost(postId, authentication);
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
        String clientIp = request.getRemoteAddr();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            clientIp = forwardedFor.split(",")[0].trim();
        }
        PostDTO post = postService.getPostById(postId, clientIp, authentication);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/getPopular")
    public ResponseEntity<List<PostDTO>> getPopularPosts(
            @RequestParam(defaultValue = "WEEKLY") TimePeriod period) {
        List<PostDTO> posts = postService.getPopularPostsByRecentModification(period, 10);
        return ResponseEntity.ok(posts);
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

    @GetMapping("/blog/{blogId}")
    public ResponseEntity<Page<PostDTO>> getPostsByBlogId(
            @PathVariable Long blogId,
            @RequestParam(required = false) String searchKeyword,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<PostDTO> posts = postService.getPosts(blogId, null, searchKeyword, pageable);
        return ResponseEntity.ok(posts);
    }
}