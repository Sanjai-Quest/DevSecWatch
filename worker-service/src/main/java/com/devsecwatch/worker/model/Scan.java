package com.devsecwatch.worker.model;

import com.devsecwatch.worker.model.enums.ScanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scans")
@EntityListeners(AuditingEntityListener.class)
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "repo_url", nullable = false, length = 500)
    private String repoUrl;

    @Column(length = 100)
    @Builder.Default
    private String branch = "main";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status;

    @Column(name = "total_files")
    @Builder.Default
    private Integer totalFiles = 0;

    @Column(name = "lines_of_code")
    @Builder.Default
    private Integer linesOfCode = 0;

    @Column(name = "total_vulnerabilities")
    @Builder.Default
    private Integer totalVulnerabilities = 0;

    @Column(name = "critical_count")
    @Builder.Default
    private Integer criticalCount = 0;

    @Column(name = "high_count")
    @Builder.Default
    private Integer highCount = 0;

    @Column(name = "medium_count")
    @Builder.Default
    private Integer mediumCount = 0;

    @Column(name = "low_count")
    @Builder.Default
    private Integer lowCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "scan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Vulnerability> vulnerabilities = new ArrayList<>();
}
