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

    private Long parentId;

    private String authorName;

    private String email;

    private List<CommentDTO> replies = new ArrayList<>();

    private boolean deleted;

    private LocalDateTime updatedAt;
}
