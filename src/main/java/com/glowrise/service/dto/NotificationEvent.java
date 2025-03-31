package com.glowrise.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private String eventType; // 알림 유형 (NEW_COMMENT, NEW_REPLY 등)
    private Long userId;      // 알림을 받을 사용자 ID
    private String message;   // 알림 메시지
    private Long postId;      // 관련 게시글 ID
    private Long commentId;   // 관련 댓글 ID (선택적)
    private Long parentId;    // 부모 댓글 ID (답글의 경우, 선택적)
}
