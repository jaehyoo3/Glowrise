package com.glowrise.web;

import com.glowrise.service.CommentService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(@RequestBody CommentDTO dto, Authentication authentication) {
        checkAuthentication(authentication);
        CommentDTO createdComment = commentService.createComment(dto, authentication);
        return ResponseEntity.ok(createdComment);
    }

    @PostMapping("/{parentId}/reply")
    public ResponseEntity<CommentDTO> createReply(
            @PathVariable Long parentId,
            @RequestBody CommentDTO dto,
            Authentication authentication) {
        checkAuthentication(authentication);
        CommentDTO createdReply = commentService.createReply(parentId, dto, authentication);
        return ResponseEntity.ok(createdReply);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentDTO dto,
            Authentication authentication) {
        checkAuthentication(authentication);
        CommentDTO updatedComment = commentService.updateComment(commentId, dto, authentication);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId,
            Authentication authentication) {
        checkAuthentication(authentication);
        commentService.deleteComment(commentId, userId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable Long postId) {
        List<CommentDTO> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<CommentDTO>> getRepliesByCommentId(@PathVariable Long commentId) {
        List<CommentDTO> replies = commentService.getRepliesByCommentId(commentId);
        return ResponseEntity.ok(replies);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentDTO> getCommentById(@PathVariable Long commentId) {
        CommentDTO comment = commentService.getCommentById(commentId);
        return ResponseEntity.ok(comment);
    }

    private void checkAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
    }
}