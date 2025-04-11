package com.glowrise.web;

import com.glowrise.service.NotificationService;
import com.glowrise.service.dto.NotificationDTO;
import com.glowrise.service.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtil securityUtil; // Added

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getNotifications(Authentication ignoredAuthentication) {
        Long userId = securityUtil.getCurrentUserIdOrThrow();
        List<NotificationDTO> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication ignoredAuthentication) {
        Long userId = securityUtil.getCurrentUserIdOrThrow();
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok().build();
    }
}