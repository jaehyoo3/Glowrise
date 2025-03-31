package com.glowrise.repository;

import com.glowrise.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndDeletedFalseAndIsReadFalseOrderByCreatedDateDesc(Long userId);

    Notification findByIdAndUserId(Long id, Long userId);
}
