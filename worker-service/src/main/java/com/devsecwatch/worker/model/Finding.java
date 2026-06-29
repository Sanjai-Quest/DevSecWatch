package com.devsecwatch.worker.model;

import com.devsecwatch.worker.model.enums.Severity;

public class Finding {
    private String filePath;
    private int lineNumber;
    private String vulnerabilityType;
    private Severity severity;
    private String description;
    private String codeSnippet;
    private String ruleId;
    private double semgrepConfidence;
    private String cveId;
    private String cweId;
    private Double cvssScore;
    private String nvdDescription;

    public Finding() {}

    public Finding(String filePath, int lineNumber, String vulnerabilityType, Severity severity, String description, 
                   String codeSnippet, String ruleId, double semgrepConfidence, String cveId, String cweId, 
                   Double cvssScore, String nvdDescription) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.vulnerabilityType = vulnerabilityType;
        this.severity = severity;
        this.description = description;
        this.codeSnippet = codeSnippet;
        this.ruleId = ruleId;
        this.semgrepConfidence = semgrepConfidence;
        this.cveId = cveId;
        this.cweId = cweId;
        this.cvssScore = cvssScore;
        this.nvdDescription = nvdDescription;
    }

    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public String getVulnerabilityType() { return vulnerabilityType; }
    public void setVulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public double getSemgrepConfidence() { return semgrepConfidence; }
    public void setSemgrepConfidence(double semgrepConfidence) { this.semgrepConfidence = semgrepConfidence; }

    public String getCveId() { return cveId; }
    public void setCveId(String cveId) { this.cveId = cveId; }

    public String getCweId() { return cweId; }
    public void setCweId(String cweId) { this.cweId = cweId; }

    public Double getCvssScore() { return cvssScore; }
    public void setCvssScore(Double cvssScore) { this.cvssScore = cvssScore; }

    public String getNvdDescription() { return nvdDescription; }
    public void setNvdDescription(String nvdDescription) { this.nvdDescription = nvdDescription; }

    public static class FindingBuilder {
        private String filePath;
        private int lineNumber;
        private String vulnerabilityType;
        private Severity severity;
        private String description;
        private String codeSnippet;
        private String ruleId;
        private double semgrepConfidence;
        private String cveId;
        private String cweId;
        private Double cvssScore;
        private String nvdDescription;

        public FindingBuilder filePath(String filePath) { this.filePath = filePath; return this; }
        public FindingBuilder lineNumber(int lineNumber) { this.lineNumber = lineNumber; return this; }
        public FindingBuilder vulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; return this; }
        public FindingBuilder severity(Severity severity) { this.severity = severity; return this; }
        public FindingBuilder description(String description) { this.description = description; return this; }
        public FindingBuilder codeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; return this; }
        public FindingBuilder ruleId(String ruleId) { this.ruleId = ruleId; return this; }
        public FindingBuilder semgrepConfidence(double semgrepConfidence) { this.semgrepConfidence = semgrepConfidence; return this; }
        public FindingBuilder cveId(String cveId) { this.cveId = cveId; return this; }
        public FindingBuilder cweId(String cweId) { this.cweId = cweId; return this; }
        public FindingBuilder cvssScore(Double cvssScore) { this.cvssScore = cvssScore; return this; }
        public FindingBuilder nvdDescription(String nvdDescription) { this.nvdDescription = nvdDescription; return this; }

        public Finding build() {
            return new Finding(filePath, lineNumber, vulnerabilityType, severity, description, codeSnippet, ruleId, 
                             semgrepConfidence, cveId, cweId, cvssScore, nvdDescription);
        }
    }

    public static FindingBuilder builder() {
        return new FindingBuilder();
    }
}
