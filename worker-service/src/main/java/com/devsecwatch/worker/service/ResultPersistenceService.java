package com.devsecwatch.worker.service;

import com.devsecwatch.worker.dto.ai.AIExplanation;
import com.devsecwatch.worker.model.*;
import com.devsecwatch.worker.model.enums.ConfidenceLevel;
import com.devsecwatch.worker.model.enums.ScanStatus;
import com.devsecwatch.worker.model.enums.Severity;
import com.devsecwatch.worker.repository.ScanMetricsRepository;
import com.devsecwatch.worker.repository.ScanRepository;
import com.devsecwatch.worker.repository.VulnerabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResultPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ResultPersistenceService.class);

    private final VulnerabilityRepository vulnerabilityRepository;
    private final ScanRepository scanRepository;
    private final ScanMetricsRepository metricsRepository;
    private final ConfidenceScoreService confidenceScoreService;

    public ResultPersistenceService(VulnerabilityRepository vulnerabilityRepository,
                                   ScanRepository scanRepository,
                                   ScanMetricsRepository metricsRepository,
                                   ConfidenceScoreService confidenceScoreService) {
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.scanRepository = scanRepository;
        this.metricsRepository = metricsRepository;
        this.confidenceScoreService = confidenceScoreService;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveScanResults(Scan scan, List<EnrichedFinding> enrichedFindings, ScanMetrics metrics) {
        log.info("Saving results for scan {}: {} vulnerabilities", scan.getId(), enrichedFindings.size());

        // Convert enriched findings to Vulnerability entities
        List<Vulnerability> vulnerabilities = enrichedFindings.stream()
                .map(ef -> buildVulnerability(scan, ef))
                .collect(Collectors.toList());

        // Save vulnerabilities (Batch)
        vulnerabilityRepository.saveAll(vulnerabilities);

        // Update aggregates
        scan.setTotalVulnerabilities(vulnerabilities.size());
        scan.setCriticalCount((int) countBySeverity(vulnerabilities, Severity.CRITICAL));
        scan.setHighCount((int) countBySeverity(vulnerabilities, Severity.HIGH));
        scan.setMediumCount((int) countBySeverity(vulnerabilities, Severity.MEDIUM));
        scan.setLowCount((int) countBySeverity(vulnerabilities, Severity.LOW));

        scan.setCompletedAt(LocalDateTime.now());
        scan.setStatus(ScanStatus.COMPLETED);

        scanRepository.save(scan);

        // Save metrics
        if (metrics != null) {
            metricsRepository.save(metrics);
        }

        log.info("Successfully saved {} vulnerabilities for scan {}", vulnerabilities.size(), scan.getId());
    }
    
    public void storeFindingsForEnrichment(Long scanId, List<Vulnerability> vulnerabilities) {
        // Placeholder for future logic if the AI worker needs a dedicated "pickup" signal
        // or for triggering the background enrichment message
        log.info("Findings stored for enrichment for scan {}", scanId);
    }

    private Vulnerability buildVulnerability(Scan scan, EnrichedFinding ef) {
        Finding finding = ef.getFinding();
        AIExplanation explanation = ef.getExplanation();

        ConfidenceLevel confidence = confidenceScoreService.calculateConfidence(finding, explanation);

        return Vulnerability.builder()
                .scan(scan)
                .filePath(truncate(finding.getFilePath(), 500))
                .lineNumber(finding.getLineNumber())
                .vulnerabilityType(truncate(finding.getVulnerabilityType(), 100))
                .severity(finding.getSeverity())
                .confidence(confidence)
                .aiStatus(com.devsecwatch.worker.model.enums.AiStatus.PENDING)
                .description(explanation.getDescription())
                .codeSnippet(finding.getCodeSnippet())
                .fixSuggestion(explanation.getFixSuggestion() != null ? explanation.getFixSuggestion()
                        : "Review code manually for security issues.")
                .cveId(truncate(finding.getCveId(), 50))
                .isTemplateExplanation(explanation.isTemplate())
                .semgrepRuleId(truncate(finding.getRuleId(), 100))
                .cvssScore(finding.getCvssScore())
                .nvdDescription(finding.getNvdDescription())
                .build();
    }

    private String truncate(String value, int maxLength) {
        if (value == null)
            return null;
        if (value.length() <= maxLength)
            return value;
        return value.substring(0, maxLength);
    }

    private long countBySeverity(List<Vulnerability> vulns, Severity severity) {
        return vulns.stream().filter(v -> v.getSeverity() == severity).count();
    }
}
