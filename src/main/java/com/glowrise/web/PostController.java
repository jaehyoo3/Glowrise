package com.glowrise.web;

import com.glowrise.service.PostService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.PostDTO;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/menu/{menuId}")
    public ResponseEntity<List<PostDTO>> getPostsByMenuId(@PathVariable Long menuId) {
        List<PostDTO> posts = postService.getPostsByMenuId(menuId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUserId(@PathVariable Long userId) {
        List<PostDTO> posts = postService.getPostsByUserId(userId);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long postId) {
        System.out.println("Received GET /api/posts/" + postId);
        PostDTO post = postService.getPostById(postId);
        if (post == null) {
            System.out.println("Post not found for ID: " + postId);
            return ResponseEntity.notFound().build();
        }
        System.out.println("Returning post: " + post);
        return ResponseEntity.ok(post);
    }

    @GetMapping("/blog/{blogId}")
    public ResponseEntity<List<PostDTO>> getAllPostsByBlogId(@PathVariable Long blogId) {
        List<PostDTO> posts = postService.getAllPostsByBlogId(blogId);
        return ResponseEntity.ok(posts);
    }

    private void checkAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}