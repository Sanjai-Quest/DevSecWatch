package com.devsecwatch.backend.listener;

import com.devsecwatch.backend.dto.notification.ScanNotification;
import com.devsecwatch.backend.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final WebSocketNotificationService notificationService;

    @RabbitListener(queues = "scan.notifications")
    public void receiveNotification(ScanNotification notification) {
        log.info("Received notification from worker for scan: {}", notification.getScanId());
        try {
            notificationService.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to forward notification to WebSocket", e);
        }
    }
}
