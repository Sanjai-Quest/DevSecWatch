package com.devsecwatch.backend.mapper;

import com.devsecwatch.backend.dto.scan.ScanResponse;
import com.devsecwatch.backend.dto.scan.VulnerabilityResponse;
import com.devsecwatch.backend.model.Scan;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class ScanMapper {
        public ScanResponse toResponse(Scan scan) {
                return ScanResponse.builder()
                                .id(scan.getId())
                                .status(scan.getStatus())
                                .repoUrl(scan.getRepoUrl())
                                .branch(scan.getBranch())
                                .totalFiles(scan.getTotalFiles())
                                .totalVulnerabilities(scan.getTotalVulnerabilities())
                                .criticalCount(scan.getCriticalCount())
                                .highCount(scan.getHighCount())
                                .mediumCount(scan.getMediumCount())
                                .lowCount(scan.getLowCount())
                                .createdAt(scan.getCreatedAt())
                                .completedAt(scan.getCompletedAt())
                                .errorMessage(scan.getErrorMessage())
                                .vulnerabilities(scan.getVulnerabilities() != null ? scan.getVulnerabilities().stream()
                                                .map(v -> VulnerabilityResponse.builder()
                                                                .id(v.getId())
                                                                .filePath(v.getFilePath())
                                                                .lineNumber(v.getLineNumber())
                                                                .vulnerabilityType(v.getVulnerabilityType())
                                                                .severity(v.getSeverity())
                                                                .confidence(v.getConfidence())
                                                                .description(v.getDescription())
                                                                .codeSnippet(v.getCodeSnippet())
                                                                .fixSuggestion(v.getFixSuggestion())
                                                                .cveId(v.getCveId())
                                                                .isTemplateExplanation(v.getIsTemplateExplanation())
                                                                .build())
                                                .collect(Collectors.toList())
                                                : Collections.emptyList())
                                .build();
        }
}
