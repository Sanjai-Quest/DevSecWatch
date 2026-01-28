package com.devsecwatch.worker.client;

import com.devsecwatch.worker.dto.ai.AIExplanation;
import com.devsecwatch.worker.dto.ai.AnalysisRequest;
import com.devsecwatch.worker.dto.ai.AnalysisResponse;
import com.devsecwatch.worker.exception.AIServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class AIServiceClient {

    private final RestTemplate restTemplate;
    private final String aiServiceUrl;

    public AIServiceClient(@Value("${ai.service.url}") String aiServiceUrl) {
        this.aiServiceUrl = aiServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    public AIExplanation getExplanation(String vulnerabilityType, String codeSnippet, String filePath, int lineNumber) {
        AnalysisRequest request = AnalysisRequest.builder()
                .vulnerabilityType(vulnerabilityType)
                .codeSnippet(codeSnippet)
                .filePath(filePath)
                .lineNumber(lineNumber)
                .build();

        try {
            ResponseEntity<AnalysisResponse> response = restTemplate.postForEntity(
                    aiServiceUrl + "/analyze",
                    request,
                    AnalysisResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                AnalysisResponse body = response.getBody();
                return AIExplanation.builder()
                        .description(body.getDescription())
                        .fixSuggestion(body.getFixSuggestion())
                        .confidence(body.getConfidence())
                        .isTemplate(body.isTemplate())
                        .build();
            } else {
                throw new AIServiceException("AI Service returned status: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("Failed to call AI service: {}", e.getMessage());
            throw new AIServiceException("Failed to call AI service", e);
        }
    }
}
