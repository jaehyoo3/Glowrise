package com.glowrise.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private Long menuId;
    private Long userId;
    private Long commentCount;
    private Long viewCount;
    private List<Long> fileIds = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer fileCount;

    public PostDTO(Long id, String title, String content, Long menuId, Long userId,
                   Long viewCount, List<Long> fileIds, LocalDateTime createdAt, LocalDateTime updatedAt, Integer fileCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.menuId = menuId;
        this.userId = userId;
        this.viewCount = viewCount;
        this.fileIds = fileIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fileCount = fileCount;
    }
}
