package com.devsecwatch.worker.dto.ai;

import java.io.Serializable;

public class AIExplanation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String description;
    private String fixSuggestion;
    private String confidence;
    private boolean isTemplate;

    public AIExplanation() {}

    public AIExplanation(String description, String fixSuggestion, String confidence, boolean isTemplate) {
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

    public static class AIExplanationBuilder {
        private String description;
        private String fixSuggestion;
        private String confidence;
        private boolean isTemplate;

        public AIExplanationBuilder description(String description) { this.description = description; return this; }
        public AIExplanationBuilder fixSuggestion(String fixSuggestion) { this.fixSuggestion = fixSuggestion; return this; }
        public AIExplanationBuilder confidence(String confidence) { this.confidence = confidence; return this; }
        public AIExplanationBuilder isTemplate(boolean isTemplate) { this.isTemplate = isTemplate; return this; }

        public AIExplanation build() {
            return new AIExplanation(description, fixSuggestion, confidence, isTemplate);
        }
    }

    public static AIExplanationBuilder builder() {
        return new AIExplanationBuilder();
    }
}
