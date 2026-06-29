package com.devsecwatch.worker.dto.message;

import java.time.LocalDateTime;

public class ScanCancellationMessage {
    private Long scanId;
    private String username;
    private String correlationId;
    private LocalDateTime timestamp;

    public ScanCancellationMessage() {}

    public ScanCancellationMessage(Long scanId, String username, String correlationId, LocalDateTime timestamp) {
        this.scanId = scanId;
        this.username = username;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
    }

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static class ScanCancellationMessageBuilder {
        private Long scanId;
        private String username;
        private String correlationId;
        private LocalDateTime timestamp;

        public ScanCancellationMessageBuilder scanId(Long scanId) { this.scanId = scanId; return this; }
        public ScanCancellationMessageBuilder username(String username) { this.username = username; return this; }
        public ScanCancellationMessageBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public ScanCancellationMessageBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public ScanCancellationMessage build() {
            return new ScanCancellationMessage(scanId, username, correlationId, timestamp);
        }
    }

    public static ScanCancellationMessageBuilder builder() {
        return new ScanCancellationMessageBuilder();
    }
}
