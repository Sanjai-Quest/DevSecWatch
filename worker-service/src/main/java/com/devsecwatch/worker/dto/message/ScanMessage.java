package com.devsecwatch.worker.dto.message;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ScanMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long scanId;
    private Long userId;
    private String repoUrl;
    private String branch;
    private String correlationId;
    private LocalDateTime timestamp;

    public ScanMessage() {}

    public ScanMessage(Long scanId, Long userId, String repoUrl, String branch, String correlationId, LocalDateTime timestamp) {
        this.scanId = scanId;
        this.userId = userId;
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
    }

    // Getters
    public Long getScanId() { return scanId; }
    public Long getUserId() { return userId; }
    public String getRepoUrl() { return repoUrl; }
    public String getBranch() { return branch; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Setters
    public void setScanId(Long scanId) { this.scanId = scanId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // Manual Builder Replacement (minimal)
    public static class ScanMessageBuilder {
        private Long scanId;
        private Long userId;
        private String repoUrl;
        private String branch;
        private String correlationId;
        private LocalDateTime timestamp;

        public ScanMessageBuilder scanId(Long scanId) { this.scanId = scanId; return this; }
        public ScanMessageBuilder userId(Long userId) { this.userId = userId; return this; }
        public ScanMessageBuilder repoUrl(String repoUrl) { this.repoUrl = repoUrl; return this; }
        public ScanMessageBuilder branch(String branch) { this.branch = branch; return this; }
        public ScanMessageBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public ScanMessageBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public ScanMessage build() {
            return new ScanMessage(scanId, userId, repoUrl, branch, correlationId, timestamp);
        }
    }

    public static ScanMessageBuilder builder() {
        return new ScanMessageBuilder();
    }
}
