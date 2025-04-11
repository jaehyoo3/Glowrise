package com.glowrise.service.util;

import com.glowrise.domain.Notification;
import com.glowrise.domain.Post;
import com.glowrise.domain.User;
import com.glowrise.domain.Comment;
import com.glowrise.domain.enumerate.NotificationType;
import com.glowrise.repository.CommentRepository;
import com.glowrise.repository.NotificationRepository;
import com.glowrise.repository.PostRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.NotificationDTO;
import com.glowrise.service.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void consumeNotification(NotificationEvent event) {
        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + event.getUserId()));

        Post post = event.getPostId() != null
                ? postRepository.findById(event.getPostId())
                .orElse(null)
                : null;

        Comment comment = event.getCommentId() != null
                ? commentRepository.findById(event.getCommentId())
                .orElse(null)
                : null;

        // Notification 엔티티 저장
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.valueOf(event.getEventType()))
                .message(event.getMessage())
                .post(post)
                .comment(comment)
                .payload(Map.of(
                        "postId", event.getPostId() != null ? event.getPostId().toString() : "",
                        "commentId", event.getCommentId() != null ? event.getCommentId().toString() : "",
                        "parentId", event.getParentId() != null ? event.getParentId().toString() : ""
                ))
                .isRead(false)
                .deleted(false)
                .build();

        notificationRepository.save(notification);

        NotificationDTO notificationDTO = getNotificationDTO(notification, post);

        messagingTemplate.convertAndSend("/topic/notifications/" + event.getUserId(), notificationDTO);
    }

    private static NotificationDTO getNotificationDTO(Notification notification, Post post) {
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setId(notification.getId());
        notificationDTO.setMessage(notification.getMessage());
        notificationDTO.setPostId(notification.getPost() != null ? notification.getPost().getId() : null);
        notificationDTO.setBlogUrl(post != null && post.getMenu() != null && post.getMenu().getBlog() != null
                ? post.getMenu().getBlog().getUrl() : "");
        notificationDTO.setMenuId(post != null && post.getMenu() != null ? post.getMenu().getId() : null);
        notificationDTO.setRead(notification.isRead());
        notificationDTO.setCreatedDate(notification.getCreatedDate());
        return notificationDTO;
    }
}