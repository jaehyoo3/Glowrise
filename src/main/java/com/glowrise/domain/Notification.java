package com.glowrise.domain;

import com.glowrise.domain.enumerate.NotificationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_read_deleted_created", columnList = "user_id, isRead, deleted, createdDate"),
        @Index(name = "idx_notification_post", columnList = "post_id"),
        @Index(name = "idx_notification_comment", columnList = "comment_id")
})
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Notification extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private boolean deleted = false;

    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsDeleted() {
        this.deleted = true;
    }
}