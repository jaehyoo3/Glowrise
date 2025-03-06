package com.glowrise.web;

import com.glowrise.service.CommentService;
import com.glowrise.service.UserService;
import com.glowrise.service.dto.CommentDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentDTO> createComment(@RequestBody CommentDTO dto) {
        CommentDTO createdComment = commentService.createComment(dto);
        return ResponseEntity.ok(createdComment);
    }

    @PostMapping("/{parentId}/reply")
    public ResponseEntity<CommentDTO> createReply(
            @PathVariable Long parentId,
            @RequestBody CommentDTO dto) {
        CommentDTO createdReply = commentService.createReply(parentId, dto);
        return ResponseEntity.ok(createdReply);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentDTO dto) {
        CommentDTO updatedComment = commentService.updateComment(commentId, dto);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
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
