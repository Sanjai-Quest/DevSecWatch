package com.devsecwatch.backend.dto.scan;

import com.devsecwatch.backend.model.enums.ScanStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ScanResponse {
    private Long id;
    private ScanStatus status;
    private String repoUrl;
    private String branch;
    private Integer totalFiles;
    private Integer totalVulnerabilities;
    private Integer criticalCount;
    private Integer highCount;
    private Integer mediumCount;
    private Integer lowCount;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private java.util.List<VulnerabilityResponse> vulnerabilities;
}
