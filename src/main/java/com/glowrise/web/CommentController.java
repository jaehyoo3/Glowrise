package com.glowrise.web;

import com.glowrise.service.CommentService;
import com.glowrise.service.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> createComment(@RequestBody CommentDTO dto, Authentication authentication) {
        CommentDTO createdComment = commentService.createComment(dto, authentication);
        return ResponseEntity.ok(createdComment);
    }

    @PostMapping("/{parentId}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> createReply(
            @PathVariable Long parentId,
            @RequestBody CommentDTO dto,
            Authentication authentication) {
        CommentDTO createdReply = commentService.createReply(parentId, dto, authentication);
        return ResponseEntity.ok(createdReply);
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentDTO dto,
            Authentication authentication) {
        CommentDTO updatedComment = commentService.updateComment(commentId, dto, authentication);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        commentService.deleteComment(commentId, authentication);
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
}