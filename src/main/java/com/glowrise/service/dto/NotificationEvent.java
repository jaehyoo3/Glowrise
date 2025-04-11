package com.glowrise.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private String eventType;
    private Long userId;
    private String message;
    private Long postId;
    private Long commentId;
    private Long parentId;
}
