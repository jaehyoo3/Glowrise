package com.glowrise.domain;

import com.glowrise.domain.enumerate.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 대상 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 알림 유형 (예: NEW_COMMENT, NEW_REPLY, SYSTEM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    // 알림 내용 (간단한 메시지)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // 관련된 게시글 (선택적)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    // 관련된 댓글 (선택적)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ElementCollection
    @MapKeyColumn(name = "payload_key") // 예약 키워드 회피를 위해 이름 변경
    @Column(name = "value")
    @CollectionTable(name = "notification_payload", joinColumns = @JoinColumn(name = "notification_id"))
    private Map<String, String> payload = new HashMap<>();

    // 읽음 여부
    @Column(nullable = false)
    private boolean isRead = false;

    // 삭제 여부 (소프트 삭제)
    @Column(nullable = false)
    private boolean deleted = false;

    // 알림 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }

    // 알림 삭제 처리
    public void markAsDeleted() {
        this.deleted = true;
    }
}