package com.devsecwatch.worker.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_metrics")
@EntityListeners(AuditingEntityListener.class)
public class ScanMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id")
    private Scan scan;

    @Column(name = "files_scanned")
    private Integer filesScanned = 0;

    @Column(name = "lines_of_code")
    private Integer linesOfCode = 0;

    @Column(name = "git_clone_duration_ms")
    private Long gitCloneDurationMs;

    @Column(name = "semgrep_duration_ms")
    private Long semgrepDurationMs;

    @Column(name = "ai_call_duration_ms")
    private Long aiCallDurationMs;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "cache_hit_rate", precision = 3, scale = 2)
    private BigDecimal cacheHitRate;

    @Column(name = "api_calls_made")
    private Integer apiCallsMade = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public ScanMetrics() {}

    public ScanMetrics(Long id, Scan scan, Integer filesScanned, Integer linesOfCode, Long gitCloneDurationMs, 
                       Long semgrepDurationMs, Long aiCallDurationMs, Long totalDurationMs, 
                       BigDecimal cacheHitRate, Integer apiCallsMade, LocalDateTime createdAt) {
        this.id = id;
        this.scan = scan;
        this.filesScanned = filesScanned != null ? filesScanned : 0;
        this.linesOfCode = linesOfCode != null ? linesOfCode : 0;
        this.gitCloneDurationMs = gitCloneDurationMs;
        this.semgrepDurationMs = semgrepDurationMs;
        this.aiCallDurationMs = aiCallDurationMs;
        this.totalDurationMs = totalDurationMs;
        this.cacheHitRate = cacheHitRate;
        this.apiCallsMade = apiCallsMade != null ? apiCallsMade : 0;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Scan getScan() { return scan; }
    public void setScan(Scan scan) { this.scan = scan; }

    public Integer getFilesScanned() { return filesScanned; }
    public void setFilesScanned(Integer filesScanned) { this.filesScanned = filesScanned; }

    public Integer getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; }

    public Long getGitCloneDurationMs() { return gitCloneDurationMs; }
    public void setGitCloneDurationMs(Long gitCloneDurationMs) { this.gitCloneDurationMs = gitCloneDurationMs; }

    public Long getSemgrepDurationMs() { return semgrepDurationMs; }
    public void setSemgrepDurationMs(Long semgrepDurationMs) { this.semgrepDurationMs = semgrepDurationMs; }

    public Long getAiCallDurationMs() { return aiCallDurationMs; }
    public void setAiCallDurationMs(Long aiCallDurationMs) { this.aiCallDurationMs = aiCallDurationMs; }

    public Long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(Long totalDurationMs) { this.totalDurationMs = totalDurationMs; }

    public BigDecimal getCacheHitRate() { return cacheHitRate; }
    public void setCacheHitRate(BigDecimal cacheHitRate) { this.cacheHitRate = cacheHitRate; }

    public Integer getApiCallsMade() { return apiCallsMade; }
    public void setApiCallsMade(Integer apiCallsMade) { this.apiCallsMade = apiCallsMade; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class ScanMetricsBuilder {
        private Long id;
        private Scan scan;
        private Integer filesScanned = 0;
        private Integer linesOfCode = 0;
        private Long gitCloneDurationMs;
        private Long semgrepDurationMs;
        private Long aiCallDurationMs;
        private Long totalDurationMs;
        private BigDecimal cacheHitRate;
        private Integer apiCallsMade = 0;
        private LocalDateTime createdAt;
        private Long totalVulnerabilities; // For ScanWorker compatibility
        private Long highSeverityCount;    // For ScanWorker compatibility

        public ScanMetricsBuilder id(Long id) { this.id = id; return this; }
        public ScanMetricsBuilder scan(Scan scan) { this.scan = scan; return this; }
        public ScanMetricsBuilder scanId(Long scanId) { 
            // Minimal shim: set a dummy scan object with ID if needed, 
            // or just ignore if it's purely for stats that aren't in this table yet.
            if (this.scan == null) this.scan = new Scan();
            this.scan.setId(scanId);
            return this; 
        }
        public ScanMetricsBuilder filesScanned(Integer filesScanned) { this.filesScanned = filesScanned; return this; }
        public ScanMetricsBuilder linesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; return this; }
        public ScanMetricsBuilder gitCloneDurationMs(Long gitCloneDurationMs) { this.gitCloneDurationMs = gitCloneDurationMs; return this; }
        public ScanMetricsBuilder semgrepDurationMs(Long semgrepDurationMs) { this.semgrepDurationMs = semgrepDurationMs; return this; }
        public ScanMetricsBuilder aiCallDurationMs(Long aiCallDurationMs) { this.aiCallDurationMs = aiCallDurationMs; return this; }
        public ScanMetricsBuilder totalDurationMs(Long totalDurationMs) { this.totalDurationMs = totalDurationMs; return this; }
        public ScanMetricsBuilder executionTimeMs(Long executionTimeMs) { this.totalDurationMs = executionTimeMs; return this; }
        public ScanMetricsBuilder cacheHitRate(BigDecimal cacheHitRate) { this.cacheHitRate = cacheHitRate; return this; }
        public ScanMetricsBuilder apiCallsMade(Integer apiCallsMade) { this.apiCallsMade = apiCallsMade; return this; }
        public ScanMetricsBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        
        // Compatibility methods
        public ScanMetricsBuilder totalVulnerabilities(Long totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; return this; }
        public ScanMetricsBuilder highSeverityCount(Long highSeverityCount) { this.highSeverityCount = highSeverityCount; return this; }

        public ScanMetrics build() {
            return new ScanMetrics(id, scan, filesScanned, linesOfCode, gitCloneDurationMs, semgrepDurationMs, 
                                 aiCallDurationMs, totalDurationMs, cacheHitRate, apiCallsMade, createdAt);
        }
    }

    public static ScanMetricsBuilder builder() {
        return new ScanMetricsBuilder();
    }
}
