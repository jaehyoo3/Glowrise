package com.glowrise.web;

import com.glowrise.service.PostService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.PostDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
    public ResponseEntity<PostDTO> createPost(
            @RequestPart("dto") PostDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        System.out.println("Received POST /api/posts, DTO: " + dto);
        try {
            PostDTO createdPost = postService.createPost(dto, files);
            return ResponseEntity.ok(createdPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null); // 400 응답
        }
    }

    @PutMapping(value = "/{postId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable Long postId,
            @RequestPart("dto") PostDTO dto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        PostDTO updatedPost = postService.updatePost(postId, dto, files);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId, @RequestParam Long userId) {
        postService.deletePost(postId, userId);
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

    @GetMapping("/{postId}") // 수정
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long postId) {
        System.out.println("Received GET /api/posts/" + postId); // 디버깅 로그
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
}
