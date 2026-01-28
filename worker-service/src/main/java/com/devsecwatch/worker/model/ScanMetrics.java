package com.devsecwatch.worker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "files_scanned", nullable = false)
    private Integer filesScanned;

    @Column(name = "lines_of_code", nullable = false)
    private Integer linesOfCode;

    @Column(name = "git_clone_duration_ms")
    private Long gitCloneDurationMs;

    @Column(name = "semgrep_duration_ms")
    private Long semgrepDurationMs;

    @Column(name = "ai_call_duration_ms")
    private Long aiCallDurationMs;

    @Column(name = "total_duration_ms", nullable = false)
    private Long totalDurationMs;

    @Column(name = "cache_hit_rate", precision = 3, scale = 2)
    private BigDecimal cacheHitRate;

    @Column(name = "api_calls_made", nullable = false)
    private Integer apiCallsMade;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
