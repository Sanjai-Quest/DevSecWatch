package com.devsecwatch.worker.service;

import com.devsecwatch.worker.dto.ai.AIExplanation;
import com.devsecwatch.worker.model.Finding;
import com.devsecwatch.worker.model.enums.ConfidenceLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ConfidenceScoreService {

    public ConfidenceLevel calculateConfidence(Finding finding, AIExplanation explanation) {
        int score = 0;

        // Factor 1: Semgrep confidence (0-3 points)
        // finding.getSemgrepConfidence() assumed to be 0.0-1.0
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
        // Template gets 0 points for "AI Quality" as it's static

        // Factor 4: Code Specificity (1 point)
        // Rough heuristic: if description contains typical code chars like () or
        // camelCase
        if (explanation != null && explanation.getDescription() != null) {
            if (explanation.getDescription().matches(".*[a-z]+[A-Z][a-z]+.*")
                    || explanation.getDescription().contains("()")) {
                score += 1;
            }
        }

        // Calculate Level
        // Max score = 3 + 2 + 2 + 1 = 8
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
