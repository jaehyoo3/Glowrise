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

    @ElementCollection
    @MapKeyColumn(name = "payload_key")
    @Column(name = "value")
    @CollectionTable(name = "notification_payload", joinColumns = @JoinColumn(name = "notification_id"))
    private Map<String, String> payload = new HashMap<>();

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