package com.glowrise.web;

import com.glowrise.service.BlogService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.BlogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    public ResponseEntity<BlogDTO> createBlog(@RequestBody BlogDTO dto, @RequestParam Long userId) {
        BlogDTO createdBlog = blogService.createBlog(dto, userId);
        return ResponseEntity.ok(createdBlog);
    }

    @PutMapping("/{blogId}")
    public ResponseEntity<BlogDTO> updateBlog(@PathVariable Long blogId, @RequestBody BlogDTO dto, @RequestParam Long userId) {
        BlogDTO updatedBlog = blogService.updateBlog(blogId, dto, userId);
        return ResponseEntity.ok(updatedBlog);
    }

    @DeleteMapping("/{blogId}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long blogId, @RequestParam Long userId) {
        blogService.deleteBlog(blogId, userId);
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
        return blog != null ? ResponseEntity.ok(blog) : ResponseEntity.notFound().build();
    }

    @GetMapping("/check-url")
    public ResponseEntity<Boolean> checkUrlAvailability(@RequestParam String url) {
        boolean available = blogService.isUrlAvailable(url);
        return ResponseEntity.ok(available);
    }

}
