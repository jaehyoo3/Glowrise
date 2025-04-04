package com.glowrise.web;

import com.glowrise.domain.Blog;
import com.glowrise.service.BlogService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.BlogDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blogs")
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    public ResponseEntity<BlogDTO> createBlog(@RequestBody BlogDTO dto, Authentication authentication) {
        checkAuthentication(authentication);
        BlogDTO createdBlog = blogService.createBlog(dto, authentication);
        return ResponseEntity.ok(createdBlog);
    }

    @PatchMapping("/{blogId}")
    public ResponseEntity<Optional<BlogDTO>> updateBlog(@PathVariable Long blogId, @RequestBody BlogDTO dto, Authentication authentication) {
        checkAuthentication(authentication);
        Optional<BlogDTO> updatedBlog = blogService.updateBlog(blogId, dto, authentication);
        return ResponseEntity.ok(updatedBlog);
    }

    @DeleteMapping("/{blogId}")
    public ResponseEntity<Void> deleteBlog(@PathVariable Long blogId, Authentication authentication) {
        checkAuthentication(authentication);
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
    public ResponseEntity<Boolean> checkUrlAvailability(@RequestParam String url, Authentication authentication) {
        checkAuthentication(authentication);
        boolean available = blogService.isUrlAvailable(url);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/{url}")
    public ResponseEntity<BlogDTO> getBlogByUrl(@PathVariable String url) {
        BlogDTO blog = blogService.getBlogByUrl(url);
        return blog != null ? ResponseEntity.ok(blog) : ResponseEntity.notFound().build();
    }

    @GetMapping("/me")
    public ResponseEntity<BlogDTO> getMyBlog(Authentication authentication) {
        checkAuthentication(authentication);
        BlogDTO blogDTO = blogService.getMyBlog(authentication);
        return ResponseEntity.ok(blogDTO);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<BlogDTO> getBlogById(@PathVariable Long id) {
        return ResponseEntity.ok(blogService.getBlogById(id));
    }

    private void checkAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}