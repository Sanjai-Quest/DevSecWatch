package com.devsecwatch.worker.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SemgrepResult {
    private List<Finding> findings;
    private int totalFindings;
    private long executionTimeMs;
}
