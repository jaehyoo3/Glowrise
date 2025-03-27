package com.glowrise.service.dto;

import com.glowrise.domain.Comment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentDTO {

    private Long id;

    private String content;

    private Long postId;

    private Long userId;

    private Long parentId; // 추가

    private String authorName; // 추가

    private String email; // 추가

    private List<CommentDTO> replies = new ArrayList<>(); // List<Comment> -> List<CommentDTO>

    private boolean deleted;

    private LocalDateTime updatedAt;
}
