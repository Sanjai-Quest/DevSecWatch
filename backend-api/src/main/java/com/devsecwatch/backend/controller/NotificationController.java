package com.devsecwatch.backend.controller;

import com.devsecwatch.backend.model.Notification;
import com.devsecwatch.backend.repository.NotificationRepository;
import com.devsecwatch.backend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(@RequestHeader("Authorization") String token) {
        String username = extractUsername(token);
        log.info("Fetching notifications for user: {}", username);
        List<Notification> notifications = notificationRepository
                .findAllByUserIdIgnoreCaseOrderByCreatedAtDesc(username);
        log.info("Found {} notifications for user: {}", notifications.size(), username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestHeader("Authorization") String token) {
        String username = extractUsername(token);
        return ResponseEntity.ok(notificationRepository.countByUserIdAndIsReadFalse(username));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        String username = extractUsername(token);
        notificationRepository.findById(id).ifPresent(notification -> {
            if (notification.getUserId().equals(username)) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("Authorization") String token) {
        String username = extractUsername(token);
        List<Notification> notifications = notificationRepository
                .findAllByUserIdIgnoreCaseOrderByCreatedAtDesc(username);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
        return ResponseEntity.ok().build();
    }

    private String extractUsername(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return jwtService.extractUsername(token.substring(7));
        }
        throw new RuntimeException("Invalid token");
    }
}
