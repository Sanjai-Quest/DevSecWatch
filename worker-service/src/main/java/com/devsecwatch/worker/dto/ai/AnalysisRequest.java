package com.devsecwatch.worker.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnalysisRequest {
    @JsonProperty("vulnerability_type")
    private String vulnerabilityType;

    @JsonProperty("code_snippet")
    private String codeSnippet;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("line_number")
    private int lineNumber;

    public AnalysisRequest() {}

    public AnalysisRequest(String vulnerabilityType, String codeSnippet, String filePath, int lineNumber) {
        this.vulnerabilityType = vulnerabilityType;
        this.codeSnippet = codeSnippet;
        this.filePath = filePath;
        this.lineNumber = lineNumber;
    }

    public String getVulnerabilityType() { return vulnerabilityType; }
    public void setVulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public static class AnalysisRequestBuilder {
        private String vulnerabilityType;
        private String codeSnippet;
        private String filePath;
        private int lineNumber;

        public AnalysisRequestBuilder vulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; return this; }
        public AnalysisRequestBuilder codeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; return this; }
        public AnalysisRequestBuilder filePath(String filePath) { this.filePath = filePath; return this; }
        public AnalysisRequestBuilder lineNumber(int lineNumber) { this.lineNumber = lineNumber; return this; }

        public AnalysisRequest build() {
            return new AnalysisRequest(vulnerabilityType, codeSnippet, filePath, lineNumber);
        }
    }

    public static AnalysisRequestBuilder builder() {
        return new AnalysisRequestBuilder();
    }
}
