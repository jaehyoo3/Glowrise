package com.glowrise.service;

import com.glowrise.domain.Notification;
import com.glowrise.repository.NotificationRepository;
import com.glowrise.repository.UserRepository;
import com.glowrise.service.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndDeletedFalseAndIsReadFalseOrderByCreatedDateDesc(userId);
        return notifications.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (notification == null) {
            throw new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId);
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    private NotificationDTO toDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setMessage(notification.getMessage());
        dto.setPostId(notification.getPost() != null ? notification.getPost().getId() : null);
        dto.setBlogUrl(notification.getPost() != null && notification.getPost().getMenu() != null && notification.getPost().getMenu().getBlog() != null
                ? notification.getPost().getMenu().getBlog().getUrl() : "");
        dto.setMenuId(notification.getPost() != null && notification.getPost().getMenu() != null ? notification.getPost().getMenu().getId() : null);
        dto.setRead(notification.isRead());
        dto.setCreatedDate(notification.getCreatedDate());
        return dto;
    }
}