package com.devsecwatch.worker.model;

import com.devsecwatch.worker.model.enums.ScanStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private String branch = "main";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScanStatus status;

    @Column(name = "total_files")
    private Integer totalFiles = 0;

    @Column(name = "lines_of_code")
    private Integer linesOfCode = 0;

    @Column(name = "total_vulnerabilities")
    private Integer totalVulnerabilities = 0;

    @Column(name = "critical_count")
    private Integer criticalCount = 0;

    @Column(name = "high_count")
    private Integer highCount = 0;

    @Column(name = "medium_count")
    private Integer mediumCount = 0;

    @Column(name = "low_count")
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
    private List<Vulnerability> vulnerabilities = new ArrayList<>();

    public Scan() {}

    public Scan(Long id, User user, String repoUrl, String branch, ScanStatus status, 
                Integer totalFiles, Integer linesOfCode, Integer totalVulnerabilities, 
                Integer criticalCount, Integer highCount, Integer mediumCount, 
                Integer lowCount, String errorMessage, LocalDateTime createdAt, 
                LocalDateTime startedAt, LocalDateTime completedAt, 
                List<Vulnerability> vulnerabilities) {
        this.id = id;
        this.user = user;
        this.repoUrl = repoUrl;
        this.branch = branch != null ? branch : "main";
        this.status = status;
        this.totalFiles = totalFiles != null ? totalFiles : 0;
        this.linesOfCode = linesOfCode != null ? linesOfCode : 0;
        this.totalVulnerabilities = totalVulnerabilities != null ? totalVulnerabilities : 0;
        this.criticalCount = criticalCount != null ? criticalCount : 0;
        this.highCount = highCount != null ? highCount : 0;
        this.mediumCount = mediumCount != null ? mediumCount : 0;
        this.lowCount = lowCount != null ? lowCount : 0;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.vulnerabilities = vulnerabilities != null ? vulnerabilities : new ArrayList<>();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public ScanStatus getStatus() { return status; }
    public void setStatus(ScanStatus status) { this.status = status; }

    public Integer getTotalFiles() { return totalFiles; }
    public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }

    public Integer getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; }

    public Integer getTotalVulnerabilities() { return totalVulnerabilities; }
    public void setTotalVulnerabilities(Integer totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; }

    public Integer getCriticalCount() { return criticalCount; }
    public void setCriticalCount(Integer criticalCount) { this.criticalCount = criticalCount; }

    public Integer getHighCount() { return highCount; }
    public void setHighCount(Integer highCount) { this.highCount = highCount; }

    public Integer getMediumCount() { return mediumCount; }
    public void setMediumCount(Integer mediumCount) { this.mediumCount = mediumCount; }

    public Integer getLowCount() { return lowCount; }
    public void setLowCount(Integer lowCount) { this.lowCount = lowCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public List<Vulnerability> getVulnerabilities() { return vulnerabilities; }
    public void setVulnerabilities(List<Vulnerability> vulnerabilities) { this.vulnerabilities = vulnerabilities; }

    public static class ScanBuilder {
        private Long id;
        private User user;
        private String repoUrl;
        private String branch = "main";
        private ScanStatus status;
        private Integer totalFiles = 0;
        private Integer linesOfCode = 0;
        private Integer totalVulnerabilities = 0;
        private Integer criticalCount = 0;
        private Integer highCount = 0;
        private Integer mediumCount = 0;
        private Integer lowCount = 0;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private List<Vulnerability> vulnerabilities = new ArrayList<>();

        public ScanBuilder id(Long id) { this.id = id; return this; }
        public ScanBuilder user(User user) { this.user = user; return this; }
        public ScanBuilder repoUrl(String repoUrl) { this.repoUrl = repoUrl; return this; }
        public ScanBuilder branch(String branch) { this.branch = branch; return this; }
        public ScanBuilder status(ScanStatus status) { this.status = status; return this; }
        public ScanBuilder totalFiles(Integer totalFiles) { this.totalFiles = totalFiles; return this; }
        public ScanBuilder linesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; return this; }
        public ScanBuilder totalVulnerabilities(Integer totalVulnerabilities) { this.totalVulnerabilities = totalVulnerabilities; return this; }
        public ScanBuilder criticalCount(Integer criticalCount) { this.criticalCount = criticalCount; return this; }
        public ScanBuilder highCount(Integer highCount) { this.highCount = highCount; return this; }
        public ScanBuilder mediumCount(Integer mediumCount) { this.mediumCount = mediumCount; return this; }
        public ScanBuilder lowCount(Integer lowCount) { this.lowCount = lowCount; return this; }
        public ScanBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public ScanBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ScanBuilder startedAt(LocalDateTime startedAt) { this.startedAt = startedAt; return this; }
        public ScanBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }
        public ScanBuilder vulnerabilities(List<Vulnerability> vulnerabilities) { this.vulnerabilities = vulnerabilities; return this; }

        public Scan build() {
            return new Scan(id, user, repoUrl, branch, status, totalFiles, linesOfCode, totalVulnerabilities, 
                          criticalCount, highCount, mediumCount, lowCount, errorMessage, createdAt, 
                          startedAt, completedAt, vulnerabilities);
        }
    }

    public static ScanBuilder builder() {
        return new ScanBuilder();
    }
}
