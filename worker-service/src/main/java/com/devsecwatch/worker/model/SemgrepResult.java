package com.devsecwatch.worker.model;

import java.util.List;

public class SemgrepResult {
    private List<Finding> findings;
    private int totalFindings;
    private long executionTimeMs;

    public SemgrepResult() {}

    public SemgrepResult(List<Finding> findings, int totalFindings, long executionTimeMs) {
        this.findings = findings;
        this.totalFindings = totalFindings;
        this.executionTimeMs = executionTimeMs;
    }

    public List<Finding> getFindings() { return findings; }
    public void setFindings(List<Finding> findings) { this.findings = findings; }

    public int getTotalFindings() { return totalFindings; }
    public void setTotalFindings(int totalFindings) { this.totalFindings = totalFindings; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public static class SemgrepResultBuilder {
        private List<Finding> findings;
        private int totalFindings;
        private long executionTimeMs;

        public SemgrepResultBuilder findings(List<Finding> findings) { this.findings = findings; return this; }
        public SemgrepResultBuilder totalFindings(int totalFindings) { this.totalFindings = totalFindings; return this; }
        public SemgrepResultBuilder executionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; return this; }

        public SemgrepResult build() {
            return new SemgrepResult(findings, totalFindings, executionTimeMs);
        }
    }

    public static SemgrepResultBuilder builder() {
        return new SemgrepResultBuilder();
    }
}
