package com.devsecwatch.worker.worker;

import com.devsecwatch.worker.config.RabbitMQConfig;
import com.devsecwatch.worker.dto.ai.AIExplanation;
import com.devsecwatch.worker.dto.message.AiEnrichmentMessage;
import com.devsecwatch.worker.model.Finding;
import com.devsecwatch.worker.model.Vulnerability;
import com.devsecwatch.worker.model.enums.AiStatus;
import com.devsecwatch.worker.repository.VulnerabilityRepository;
import com.devsecwatch.worker.service.AIEnrichmentService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Component
public class AiEnrichmentWorker {

    private static final Logger log = LoggerFactory.getLogger(AiEnrichmentWorker.class);

    private final VulnerabilityRepository vulnerabilityRepository;
    private final AIEnrichmentService aiEnrichmentService;

    public AiEnrichmentWorker(VulnerabilityRepository vulnerabilityRepository, 
                              AIEnrichmentService aiEnrichmentService) {
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.aiEnrichmentService = aiEnrichmentService;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_ENRICHMENT_QUEUE, ackMode = "MANUAL")
    @Transactional
    public void processEnrichment(@Payload AiEnrichmentMessage message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        Long scanId = message.getScanId();
        log.info("Starting background AI enrichment for scan ID: {}", scanId);

        try {
            List<Vulnerability> vulnerabilities = vulnerabilityRepository.findByScanId(scanId);
            
            if (vulnerabilities.isEmpty()) {
                log.warn("No vulnerabilities found for scan ID: {}. Acking job.", scanId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            for (Vulnerability vuln : vulnerabilities) {
                if (vuln.getAiStatus() == AiStatus.COMPLETED) continue;

                // Reconstruct a Finding object for the enrichment service
                Finding finding = Finding.builder()
                        .vulnerabilityType(vuln.getVulnerabilityType())
                        .filePath(vuln.getFilePath())
                        .lineNumber(vuln.getLineNumber())
                        .severity(vuln.getSeverity())
                        .codeSnippet(vuln.getCodeSnippet())
                        .build();

                try {
                    // Reuse existing enrichment service logic
                    List<com.devsecwatch.worker.model.EnrichedFinding> results = aiEnrichmentService.enrichFindings(List.of(finding));
                    
                    if (!results.isEmpty()) {
                        AIExplanation explanation = results.get(0).getExplanation();
                        
                        if (explanation.isTemplate()) {
                             if ("TEMPLATE".equals(explanation.getConfidence())) {
                                 setFailed(vuln);
                             } else {
                                 setCompleted(vuln, explanation);
                             }
                        } else {
                            setCompleted(vuln, explanation);
                        }
                    } else {
                        setFailed(vuln);
                    }
                } catch (Exception e) {
                    log.error("AI Enrichment failed for vulnerability {}: {}", vuln.getId(), e.getMessage());
                    setFailed(vuln);
                }
            }

            vulnerabilityRepository.saveAll(vulnerabilities);
            log.info("Completed background AI enrichment for scan ID: {}", scanId);
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("Critical error in AI enrichment worker for scan ID: {}: {}", scanId, e.getMessage(), e);
            // Requeue only for unexpected infrastructure errors
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private void setCompleted(Vulnerability vuln, AIExplanation explanation) {
        vuln.setDescription(explanation.getDescription());
        vuln.setFixSuggestion(explanation.getFixSuggestion());
        vuln.setAiStatus(AiStatus.COMPLETED);
        vuln.setIsTemplateExplanation(explanation.isTemplate());
    }

    private void setFailed(Vulnerability vuln) {
        vuln.setAiStatus(AiStatus.FAILED);
        // Keep the original description
        vuln.setFixSuggestion("AI analysis unavailable. Please review the vulnerability type and code snippet manually.");
    }
}
