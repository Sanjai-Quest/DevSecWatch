package com.devsecwatch.worker.service;

import com.devsecwatch.worker.client.AIServiceClient;
import com.devsecwatch.worker.dto.ai.AIExplanation;
import com.devsecwatch.worker.exception.AIServiceException;
import com.devsecwatch.worker.model.EnrichedFinding;
import com.devsecwatch.worker.model.Finding;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIEnrichmentService {

    private final AIServiceClient aiClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "explanation_v3:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private static final Map<String, AIExplanation> TEMPLATES = Map.of(
            "SQL_INJECTION", AIExplanation.builder()
                    .description("SQL Injection detected. User input is concatenated directly into SQL queries.")
                    .fixSuggestion("Use PreparedStatement with placeholders (?) instead of concatenation.")
                    .confidence("TEMPLATE")
                    .isTemplate(true).build(),
            "DEFAULT", AIExplanation.builder()
                    .description("Potential security vulnerability detected.")
                    .fixSuggestion("Review code manually using OWASP guidelines.")
                    .confidence("TEMPLATE")
                    .isTemplate(true).build());

    public List<EnrichedFinding> enrichFindings(List<Finding> findings) {
        List<EnrichedFinding> result = new ArrayList<>();
        int cacheHits = 0;

        for (Finding finding : findings) {
            AIExplanation explanation = null;
            String snippet = finding.getCodeSnippet() != null ? finding.getCodeSnippet() : "";
            // Use vulnerability type + MD5 of snippet to form key
            String cacheKey = CACHE_PREFIX + finding.getVulnerabilityType() + ":" + DigestUtils.md5Hex(snippet);

            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    if (cached instanceof AIExplanation) {
                        explanation = (AIExplanation) cached;
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        explanation = mapper.convertValue(cached, AIExplanation.class);
                    }
                    cacheHits++;
                }
            } catch (Exception e) {
                log.warn("Redis cache read error: {}", e.getMessage());
            }

            if (explanation == null) {
                try {
                    explanation = aiClient.getExplanation(
                            finding.getVulnerabilityType(),
                            snippet,
                            finding.getFilePath(),
                            finding.getLineNumber());

                    try {
                        redisTemplate.opsForValue().set(cacheKey, explanation, CACHE_TTL);
                    } catch (Exception e) {
                        log.warn("Redis cache write error: {}", e.getMessage());
                    }

                } catch (AIServiceException e) {
                    log.warn("AI service failed for {}: {}, using template", finding.getVulnerabilityType(),
                            e.getMessage());
                    explanation = getTemplateExplanation(finding.getVulnerabilityType());
                }
            }

            result.add(EnrichedFinding.builder()
                    .finding(finding)
                    .explanation(explanation)
                    .build());
        }

        double hitRate = findings.isEmpty() ? 0 : (double) cacheHits / findings.size();
        log.info("AI enrichment complete: {} findings, {}% cache hit rate", findings.size(),
                String.format("%.2f", hitRate * 100));

        return result;
    }

    private AIExplanation getTemplateExplanation(String vulnerabilityType) {
        return TEMPLATES.getOrDefault(vulnerabilityType, TEMPLATES.get("DEFAULT"));
    }

    // Helper to expose hit rate logic if needed (optional)
    public double calculateHitRate(int hits, int total) {
        return total == 0 ? 0 : (double) hits / total;
    }
}
