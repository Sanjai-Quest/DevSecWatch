package com.devsecwatch.backend.service;

import com.devsecwatch.backend.dto.notification.ScanNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final com.devsecwatch.backend.repository.NotificationRepository notificationRepository;

    public void sendNotification(ScanNotification notification) {
        String username = notification.getUserId();

        // 1. Persist Notification
        try {
            com.devsecwatch.backend.model.Notification entity = com.devsecwatch.backend.model.Notification.builder()
                    .userId(username)
                    .title(getNotificationTitle(notification))
                    .message(notification.getMessage())
                    .type(getNotificationType(notification))
                    .scanId(notification.getScanId())
                    .createdAt(java.time.LocalDateTime.now())
                    .isRead(false)
                    .build();

            notificationRepository.save(entity);
            log.info("Persisted notification for user {}", username);
        } catch (Exception e) {
            log.error("Failed to persist notification", e);
        }

        // 2. Send to WebSocket
        messagingTemplate.convertAndSend(
                "/queue/notifications/" + username.toLowerCase(),
                notification);

        log.info("WebSocket notification sent for scan {} to user {}", notification.getScanId(), username);
    }

    private String getNotificationTitle(ScanNotification n) {
        if ("COMPLETED".equals(n.getStatus().toString())) {
            return n.getTotalVulnerabilities() > 0 ? "Scan Completed - Issues Found" : "Scan Completed - Secure";
        } else if ("FAILED".equals(n.getStatus().toString())) {
            return "Scan Failed";
        }
        return "Scan Update";
    }

    private String getNotificationType(ScanNotification n) {
        if ("COMPLETED".equals(n.getStatus().toString())) {
            return n.getTotalVulnerabilities() > 0 ? "WARNING" : "SUCCESS";
        } else if ("FAILED".equals(n.getStatus().toString())) {
            return "ERROR";
        }
        return "INFO";
    }
}
