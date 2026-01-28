package com.devsecwatch.worker.dto.notification;

import com.devsecwatch.worker.model.enums.ScanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
