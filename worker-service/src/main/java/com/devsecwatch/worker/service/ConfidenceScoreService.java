package com.devsecwatch.worker.service;

import com.devsecwatch.worker.dto.ai.AIExplanation;
import com.devsecwatch.worker.model.Finding;
import com.devsecwatch.worker.model.enums.ConfidenceLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConfidenceScoreService {

    private static final Logger log = LoggerFactory.getLogger(ConfidenceScoreService.class);

    public ConfidenceLevel calculateConfidence(Finding finding, AIExplanation explanation) {
        int score = 0;

        // Factor 1: Semgrep confidence (0-3 points)
        double semgrepConf = finding.getSemgrepConfidence();
        if (semgrepConf >= 0.9)
            score += 3;
        else if (semgrepConf >= 0.7)
            score += 2;
        else
            score += 1;

        // Factor 2: Known CVE (2 points)
        if (finding.getCveId() != null && !finding.getCveId().isEmpty()) {
            score += 2;
        }

        // Factor 3: AI Quality (0-2 points)
        if (explanation != null && !explanation.isTemplate()) {
            if (explanation.getDescription() != null && explanation.getDescription().length() > 150) {
                score += 2;
            } else {
                score += 1;
            }
        }

        // Factor 4: Code Specificity (1 point)
        if (explanation != null && explanation.getDescription() != null) {
            if (explanation.getDescription().matches(".*[a-z]+[A-Z][a-z]+.*")
                    || explanation.getDescription().contains("()")) {
                score += 1;
            }
        }

        // Calculate Level
        ConfidenceLevel level;
        if (score >= 7) {
            level = ConfidenceLevel.HIGH;
        } else if (score >= 4) {
            level = ConfidenceLevel.MEDIUM;
        } else {
            level = ConfidenceLevel.LOW;
        }

        log.debug("Confidence score for {}: {} -> {}", finding.getVulnerabilityType(), score, level);
        return level;
    }
}
