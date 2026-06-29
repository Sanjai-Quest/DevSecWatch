package com.devsecwatch.worker.dto.message;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AiEnrichmentMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long scanId;
    private Long userId;
    private String correlationId;
    private LocalDateTime timestamp;

    public AiEnrichmentMessage() {}

    public AiEnrichmentMessage(Long scanId, Long userId, String correlationId, LocalDateTime timestamp) {
        this.scanId = scanId;
        this.userId = userId;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
    }

    public Long getScanId() { return scanId; }
    public void setScanId(Long scanId) { this.scanId = scanId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return; }

    public static class AiEnrichmentMessageBuilder {
        private Long scanId;
        private Long userId;
        private String correlationId;
        private LocalDateTime timestamp;

        public AiEnrichmentMessageBuilder scanId(Long scanId) { this.scanId = scanId; return this; }
        public AiEnrichmentMessageBuilder userId(Long userId) { this.userId = userId; return this; }
        public AiEnrichmentMessageBuilder correlationId(String correlationId) { this.correlationId = correlationId; return this; }
        public AiEnrichmentMessageBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AiEnrichmentMessage build() {
            return new AiEnrichmentMessage(scanId, userId, correlationId, timestamp);
        }
    }

    public static AiEnrichmentMessageBuilder builder() {
        return new AiEnrichmentMessageBuilder();
    }
}
