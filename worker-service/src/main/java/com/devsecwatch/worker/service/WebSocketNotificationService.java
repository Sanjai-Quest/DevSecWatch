package com.devsecwatch.worker.service;

import com.devsecwatch.worker.dto.notification.ScanNotification;
import com.devsecwatch.worker.model.Scan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WebSocketNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final RabbitTemplate rabbitTemplate;
    private final com.devsecwatch.worker.repository.UserRepository userRepository;

    @Value("${rabbitmq.queue.notifications:scan.notifications}")
    private String notificationQueue;

    public WebSocketNotificationService(RabbitTemplate rabbitTemplate, 
                                        com.devsecwatch.worker.repository.UserRepository userRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.userRepository = userRepository;
    }

    public void notifyScanUpdate(Scan scan, String message) {
        // Compatibility method for ScanWorker calls
        log.info("Scan status update for {}: {}", scan.getId(), message);
        notifyScanComplete(scan); // Reuse notification logic
    }

    public void notifyScanComplete(Scan scan) {
        String username = "unknown";
        try {
            if (scan.getUser() != null) {
                // Try to get username from the potentially proxied User object
                username = scan.getUser().getUsername();
                if (username == null) {
                    // Fallback to fetch by ID
                    username = userRepository.findById(scan.getUser().getId())
                            .map(com.devsecwatch.worker.model.User::getUsername)
                            .orElse("unknown");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve username from Scan object directly, trying fallback via ID", e);
            try {
                if (scan.getUser() != null && scan.getUser().getId() != null) {
                    username = userRepository.findById(scan.getUser().getId())
                            .map(com.devsecwatch.worker.model.User::getUsername)
                            .orElse("unknown");
                }
            } catch (Exception ex) {
                log.error("Failed to retrieve user for scan notification", ex);
            }
        }

        ScanNotification notification = ScanNotification.builder()
                .scanId(scan.getId())
                .repoUrl(scan.getRepoUrl())
                .status(scan.getStatus())
                .totalVulnerabilities(scan.getTotalVulnerabilities())
                .criticalCount(scan.getCriticalCount())
                .highCount(scan.getHighCount())
                .message(buildMessage(scan))
                .timestamp(LocalDateTime.now())
                .userId(username)
                .build();

        rabbitTemplate.convertAndSend(notificationQueue, notification);
        log.info("Notification event published for scan {} for user {}", scan.getId(), username);
    }

    private String buildMessage(Scan scan) {
        if (scan.getStatus() == com.devsecwatch.worker.model.enums.ScanStatus.COMPLETED) {
            if (scan.getTotalVulnerabilities() == 0) {
                return "Scan completed successfully! No vulnerabilities found.";
            } else {
                return String.format("Scan completed! Found %d vulnerabilities (%d critical, %d high)",
                        scan.getTotalVulnerabilities(),
                        scan.getCriticalCount(),
                        scan.getHighCount());
            }
        } else if (scan.getStatus() == com.devsecwatch.worker.model.enums.ScanStatus.FAILED) {
            return "Scan failed: " + scan.getErrorMessage();
        }
        return "Scan status updated";
    }
}
