package com.glowrise.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostListDTO {
    private Long id;
    private String title;
    private String content;
    private Long menuId;
    private Long userId;
    private List<Long> commentsId = new ArrayList<>();
    private Long commentCount;
    private List<Long> fileIds = new ArrayList<>();
    private Long viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
