package com.devsecwatch.worker.dto.notification;

import com.devsecwatch.worker.model.enums.ScanStatus;
import java.time.LocalDateTime;

public class ScanNotification {
    private Long scanId;
    private String repoUrl;
    private ScanStatus status;
    private Integer totalVulnerabilities;
    private Integer criticalCount;
    private Integer highCount;
    private String message;
    private LocalDateTime timestamp;
    private String userId;

    public ScanNotification() {}

    public ScanNotification(Long scanId, String repoUrl, ScanStatus status, Integer totalVulnerabilities, 
                            Integer criticalCount, Integer highCount, String message, 
                            LocalDateTime timestamp, String userId) {
        this.scanId = scanId;
        this.repoUrl = repoUrl;
        this.status = status;
        this.totalVulnerabilities = totalVulnerabilities;
        this.criticalCount = criticalCount;
        this.highCount = highCount;
        this.message = message;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public ScanStatus getStatus() { return status; }
    public void setStatus(ScanStatus status) { this.status = status; }

    public Integer getTotalVulnerabilities() { return totalVulnerabilities; }
    public void setTotalVulnerabilities(Integer totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; }

    public Integer getCriticalCount() { return criticalCount; }
    public void setCriticalCount(Integer criticalCount) { this.criticalCount = criticalCount; }

    public Integer getHighCount() { return highCount; }
    public void setHighCount(Integer highCount) { this.highCount = highCount; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public static class ScanNotificationBuilder {
        private Long scanId;
        private String repoUrl;
        private ScanStatus status;
        private Integer totalVulnerabilities;
        private Integer criticalCount;
        private Integer highCount;
        private String message;
        private LocalDateTime timestamp;
        private String userId;

        public ScanNotificationBuilder scanId(Long scanId) { this.scanId = scanId; return this; }
        public ScanNotificationBuilder repoUrl(String repoUrl) { this.repoUrl = repoUrl; return this; }
        public ScanNotificationBuilder status(ScanStatus status) { this.status = status; return this; }
        public ScanNotificationBuilder totalVulnerabilities(Integer totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; return this; }
        public ScanNotificationBuilder criticalCount(Integer criticalCount) { this.criticalCount = criticalCount; return this; }
        public ScanNotificationBuilder highCount(Integer highCount) { this.highCount = highCount; return this; }
        public ScanNotificationBuilder message(String message) { this.message = message; return this; }
        public ScanNotificationBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public ScanNotificationBuilder userId(String userId) { this.userId = userId; return this; }

        public ScanNotification build() {
            return new ScanNotification(scanId, repoUrl, status, totalVulnerabilities, criticalCount, 
                                      highCount, message, timestamp, userId);
        }
    }

    public static ScanNotificationBuilder builder() {
        return new ScanNotificationBuilder();
    }
}
