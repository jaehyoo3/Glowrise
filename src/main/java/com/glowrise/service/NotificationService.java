package com.glowrise.service;

import com.glowrise.domain.Notification;
import com.glowrise.repository.NotificationRepository;
import com.glowrise.service.dto.NotificationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j // 로깅 추가
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndDeletedFalseAndIsReadFalseOrderByCreatedDateDesc(userId);
        return notifications.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (notification == null) {
            log.warn("알림을 찾을 수 없습니다. userId={}, notificationId={}", userId, notificationId);
            throw new IllegalArgumentException("알림을 찾을 수 없습니다: " + notificationId);
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
            log.info("알림 읽음 처리 완료. userId={}, notificationId={}", userId, notificationId);
        } else {
            log.info("이미 읽음 처리된 알림입니다. userId={}, notificationId={}", userId, notificationId);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        int updatedCount = notificationRepository.markAllAsReadForUser(userId);
        log.info("사용자 {}의 알림 {}개를 읽음 처리했습니다.", userId, updatedCount);
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