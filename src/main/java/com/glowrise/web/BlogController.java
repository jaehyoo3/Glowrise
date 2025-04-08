package com.glowrise.web;

import com.glowrise.service.BlogService;
import com.glowrise.service.dto.BlogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BlogDTO> createBlog(@RequestBody BlogDTO dto, Authentication authentication) {
        BlogDTO createdBlog = blogService.createBlog(dto, authentication);
        return ResponseEntity.ok(createdBlog);
    }

    @PatchMapping("/{blogId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BlogDTO> updateBlog(
            @PathVariable Long blogId,
            @RequestBody BlogDTO dto,
            Authentication authentication) {
        BlogDTO updatedBlog = blogService.updateBlog(blogId, dto, authentication);
        return ResponseEntity.ok(updatedBlog);
    }

    @DeleteMapping("/{blogId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long blogId, Authentication authentication) {
        blogService.deleteBlog(blogId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<BlogDTO>> getAllBlogs() {
        List<BlogDTO> blogs = blogService.getAllBlogs();
        return ResponseEntity.ok(blogs);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<BlogDTO> getBlogByUserId(@PathVariable Long userId) {
        BlogDTO blog = blogService.getBlogByUserId(userId);
        return ResponseEntity.ok(blog);
    }

    @GetMapping("/check-url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> checkUrlAvailability(@RequestParam String url, Authentication authentication) {
        boolean available = blogService.isUrlAvailable(url);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/{url}")
    public ResponseEntity<BlogDTO> getBlogByUrl(@PathVariable String url) {
        BlogDTO blog = blogService.getBlogByUrl(url);
        return blog != null ? ResponseEntity.ok(blog) : ResponseEntity.notFound().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BlogDTO> getMyBlog(Authentication authentication) {
        BlogDTO blogDTO = blogService.getMyBlog(authentication);
        return ResponseEntity.ok(blogDTO);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<BlogDTO> getBlogById(@PathVariable Long id) {
        BlogDTO blog = blogService.getBlogById(id);
        return ResponseEntity.ok(blog);
    }
}