package com.glowrise.service.util;

import com.glowrise.domain.Comment;
import com.glowrise.domain.Notification;
import com.glowrise.domain.Post;
import com.glowrise.domain.User;
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
                .orElse(null) // 게시글이 없을 수도 있으므로 null 처리 유지
                : null;

        Comment comment = event.getCommentId() != null
                ? commentRepository.findById(event.getCommentId())
                .orElse(null) // 댓글이 없을 수도 있으므로 null 처리 유지
                : null;

        // Notification 엔티티 저장
        Notification notification = Notification.builder()
                .user(user)
                .type(NotificationType.valueOf(event.getEventType()))
                .message(event.getMessage())
                .post(post) // Post 엔티티 직접 설정
                .comment(comment) // Comment 엔티티 직접 설정
                // --- .payload(...) 부분 제거 ---
                .isRead(false)
                .deleted(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification); // 저장된 엔티티 받기

        // DTO 변환 시 savedNotification 사용
        NotificationDTO notificationDTO = getNotificationDTO(savedNotification); // 메소드 시그니처 변경

        messagingTemplate.convertAndSend("/topic/notifications/" + event.getUserId(), notificationDTO);
    }

    // getNotificationDTO 메소드에서 Post 파라미터 제거
    private NotificationDTO getNotificationDTO(Notification notification) {
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setId(notification.getId());
        notificationDTO.setMessage(notification.getMessage());
        notificationDTO.setPostId(notification.getPost() != null ? notification.getPost().getId() : null);

        // BlogUrl 및 MenuId 로직은 Notification 객체에서 직접 가져오도록 유지
        String blogUrl = "";
        Long menuId = null;
        // Null 체크 강화
        if (notification.getPost() != null && notification.getPost().getMenu() != null) {
            menuId = notification.getPost().getMenu().getId();
            if (notification.getPost().getMenu().getBlog() != null) {
                blogUrl = notification.getPost().getMenu().getBlog().getUrl();
            }
        }
        notificationDTO.setBlogUrl(blogUrl);
        notificationDTO.setMenuId(menuId);

        notificationDTO.setRead(notification.isRead());
        notificationDTO.setCreatedDate(notification.getCreatedDate());
        return notificationDTO;
    }
}