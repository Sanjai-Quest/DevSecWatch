package com.devsecwatch.backend.controller;

import com.devsecwatch.backend.dto.notification.ScanNotification;
import com.devsecwatch.backend.model.enums.ScanStatus;
import com.devsecwatch.backend.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestNotificationController {

    private final WebSocketNotificationService notificationService;

    @PostMapping("/notify")
    public ResponseEntity<String> sendTestNotification(@RequestParam String username) {
        ScanNotification notification = ScanNotification.builder()
                .scanId(999L)
                .repoUrl("https://github.com/test/repo")
                .status(ScanStatus.COMPLETED)
                .totalVulnerabilities(5)
                .criticalCount(1)
                .highCount(2)
                .message("Test notification from Smoke Test Controller")
                .timestamp(LocalDateTime.now())
                .userId(username)
                .build();

        notificationService.sendNotification(notification);
        return ResponseEntity.ok("Notification sent to " + username);
    }
}
