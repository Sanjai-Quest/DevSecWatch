package com.devsecwatch.backend.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatContext {
    private Integer totalScans;
    private Integer totalVulnerabilities;
    private Map<String, Integer> severityCounts;
    private List<String> topVulnerabilityTypes;
    private List<String> repositories;
    private String recentScanSummary;
}
