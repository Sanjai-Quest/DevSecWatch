package com.devsecwatch.worker.dto.ai;

public class AnalysisResponse {
    private String description;
    private String fixSuggestion;
    private String confidence;
    private boolean isTemplate;

    public AnalysisResponse() {}

    public AnalysisResponse(String description, String fixSuggestion, String confidence, boolean isTemplate) {
        this.description = description;
        this.fixSuggestion = fixSuggestion;
        this.confidence = confidence;
        this.isTemplate = isTemplate;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFixSuggestion() { return fixSuggestion; }
    public void setFixSuggestion(String fixSuggestion) { this.fixSuggestion = fixSuggestion; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public boolean isTemplate() { return isTemplate; }
    public void setTemplate(boolean isTemplate) { this.isTemplate = isTemplate; }

    public static class AnalysisResponseBuilder {
        private String description;
        private String fixSuggestion;
        private String confidence;
        private boolean isTemplate;

        public AnalysisResponseBuilder description(String description) { this.description = description; return this; }
        public AnalysisResponseBuilder fixSuggestion(String fixSuggestion) { this.fixSuggestion = fixSuggestion; return this; }
        public AnalysisResponseBuilder confidence(String confidence) { this.confidence = confidence; return this; }
        public AnalysisResponseBuilder isTemplate(boolean isTemplate) { this.isTemplate = isTemplate; return this; }

        public AnalysisResponse build() {
            return new AnalysisResponse(description, fixSuggestion, confidence, isTemplate);
        }
    }

    public static AnalysisResponseBuilder builder() {
        return new AnalysisResponseBuilder();
    }
}
