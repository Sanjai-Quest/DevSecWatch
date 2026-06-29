package com.devsecwatch.worker.model;

import com.devsecwatch.worker.dto.ai.AIExplanation;

public class EnrichedFinding {
    private Finding finding;
    private AIExplanation explanation;

    public EnrichedFinding() {}

    public EnrichedFinding(Finding finding, AIExplanation explanation) {
        this.finding = finding;
        this.explanation = explanation;
    }

    public Finding getFinding() { return finding; }
    public void setFinding(Finding finding) { this.finding = finding; }

    public AIExplanation getExplanation() { return explanation; }
    public void setExplanation(AIExplanation explanation) { this.explanation = explanation; }

    public static class EnrichedFindingBuilder {
        private Finding finding;
        private AIExplanation explanation;

        public EnrichedFindingBuilder finding(Finding finding) { this.finding = finding; return this; }
        public EnrichedFindingBuilder explanation(AIExplanation explanation) { this.explanation = explanation; return this; }

        public EnrichedFinding build() {
            return new EnrichedFinding(finding, explanation);
        }
    }

    public static EnrichedFindingBuilder builder() {
        return new EnrichedFindingBuilder();
    }
}
