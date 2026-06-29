package com.devsecwatch.worker.worker;

import com.devsecwatch.worker.dto.message.ScanMessage;
import com.devsecwatch.worker.model.Scan;
import com.devsecwatch.worker.model.Vulnerability;
import com.devsecwatch.worker.model.ScanMetrics;
import com.devsecwatch.worker.model.enums.ScanStatus;
import com.devsecwatch.worker.model.enums.Severity;
import com.devsecwatch.worker.model.enums.AiStatus;
import com.devsecwatch.worker.model.enums.ConfidenceLevel;
import com.devsecwatch.worker.model.SemgrepResult;
import com.devsecwatch.worker.repository.ScanRepository;
import com.devsecwatch.worker.repository.VulnerabilityRepository;
import com.devsecwatch.worker.repository.ScanMetricsRepository;
import com.devsecwatch.worker.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Component
public class ScanWorker {

    private static final Logger log = LoggerFactory.getLogger(ScanWorker.class);

    private final ScanRepository scanRepository;
    private final VulnerabilityRepository vulnerabilityRepository;
    private final GitService gitService;
    private final SemgrepService semgrepService;
    private final DependencyScanner dependencyScanner;
    private final WebSocketNotificationService notificationService;
    private final ResultPersistenceService persistenceService;
    private final ScanMetricsRepository metricsRepository;
    private final ScanProcessRegistry processRegistry;

    public ScanWorker(ScanRepository scanRepository,
                      VulnerabilityRepository vulnerabilityRepository,
                      GitService gitService,
                      SemgrepService semgrepService,
                      DependencyScanner dependencyScanner,
                      WebSocketNotificationService notificationService,
                      ResultPersistenceService persistenceService,
                      ScanMetricsRepository metricsRepository,
                      ScanProcessRegistry processRegistry) {
        this.scanRepository = scanRepository;
        this.vulnerabilityRepository = vulnerabilityRepository;
        this.gitService = gitService;
        this.semgrepService = semgrepService;
        this.dependencyScanner = dependencyScanner;
        this.notificationService = notificationService;
        this.persistenceService = persistenceService;
        this.metricsRepository = metricsRepository;
        this.processRegistry = processRegistry;
    }

    @RabbitListener(queues = "${rabbitmq.queue.scans}")
    public void processScan(ScanMessage message) {
        log.info("Received scan request for project: {} (Scan ID: {})", message.getRepoUrl(), message.getScanId());
        
        Long scanId = message.getScanId();

        // Register thread for cancellation
        processRegistry.registerThread(scanId, Thread.currentThread());

        // Check for early cancellation
        if (processRegistry.isCancelled(scanId)) {
            log.info("Scan {} was cancelled before starting", scanId);
            processRegistry.unregister(scanId);
            return;
        }

        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) {
            log.error("Scan entity not found for ID: {}", scanId);
            processRegistry.unregister(scanId);
            return;
        }

        // Prevent processing if already in terminal state
        if (scan.getStatus() == ScanStatus.COMPLETED || scan.getStatus() == ScanStatus.FAILED) {
            log.warn("Scan {} is already in state {}, skipping", scanId, scan.getStatus());
            processRegistry.unregister(scanId);
            return;
        }

        try {
            updateScanStatus(scan, ScanStatus.PROCESSING);
            notificationService.notifyScanUpdate(scan, "Scan started...");

            Path repoPath = gitService.cloneRepository(message.getRepoUrl(), message.getBranch(), scanId);
            checkCancellation(scanId);

            // 1. Static Analysis (Semgrep)
            notificationService.notifyScanUpdate(scan, "Running static analysis...");
            SemgrepResult result = semgrepService.runScan(repoPath, scanId);
            checkCancellation(scanId);

            // 2. Dependency Scanning
            notificationService.notifyScanUpdate(scan, "Scanning dependencies...");
            List<Vulnerability> depVulnerabilities = dependencyScanner.scan(repoPath);
            checkCancellation(scanId);

            // 3. Process & Persist Findings
            notificationService.notifyScanUpdate(scan, "Summarizing results...");
            
            // Map Semgrep findings to our model
            List<Vulnerability> semgrepVulnerabilities = result.getFindings().stream()
                    .map(finding -> Vulnerability.builder()
                            .scan(scan)
                            .filePath(finding.getFilePath())
                            .lineNumber(finding.getLineNumber())
                            .severity(finding.getSeverity())
                            .description(finding.getDescription())
                            .vulnerabilityType(finding.getVulnerabilityType())
                            .codeSnippet(finding.getCodeSnippet())
                            .ruleId(finding.getRuleId())
                            .aiStatus(AiStatus.PENDING)
                            .confidence(ConfidenceLevel.MEDIUM)
                            .build())
                    .collect(Collectors.toList());

            List<Vulnerability> allVuls = new ArrayList<>(semgrepVulnerabilities);
            
            // Set scan property on dependency vulnerabilities
            for (Vulnerability v : depVulnerabilities) {
                v.setScan(scan);
            }
            allVuls.addAll(depVulnerabilities);

            // Update NVD Description if available from Semgrep
            for (Vulnerability v : allVuls) {
                if (v.getDescription() == null || v.getDescription().isEmpty()) {
                    v.setDescription("Detailed analysis pending...");
                }
            }

            vulnerabilityRepository.saveAll(allVuls);
            
            // Store results for AI worker to pick up
            persistenceService.storeFindingsForEnrichment(scanId, allVuls);

            // Record Metrics
            ScanMetrics metrics = ScanMetrics.builder()
                    .scanId(scanId)
                    .totalVulnerabilities((long) allVuls.size())
                    .highSeverityCount(allVuls.stream().filter(v -> v.getSeverity() == Severity.HIGH || v.getSeverity() == Severity.CRITICAL).count())
                    .executionTimeMs(System.currentTimeMillis() - scan.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                    .build();
            metricsRepository.save(metrics);

            // Update Scan entity with summary counts
            scan.setTotalVulnerabilities(allVuls.size());
            scan.setCriticalCount((int) allVuls.stream().filter(v -> v.getSeverity() == Severity.CRITICAL).count());
            scan.setHighCount((int) allVuls.stream().filter(v -> v.getSeverity() == Severity.HIGH).count());
            scan.setMediumCount((int) allVuls.stream().filter(v -> v.getSeverity() == Severity.MEDIUM).count());
            scan.setLowCount((int) allVuls.stream().filter(v -> v.getSeverity() == Severity.LOW).count());
            scan.setCompletedAt(LocalDateTime.now());
            
            updateScanStatus(scan, ScanStatus.COMPLETED);
            notificationService.notifyScanUpdate(scan, "Scan completed successfully. " + allVuls.size() + " findings.");

            // Cleanup
            gitService.cleanup(repoPath);

        } catch (Exception e) {
            if (processRegistry.isCancelled(scanId) || (e.getCause() != null && e.getCause() instanceof InterruptedException)) {
                log.info("Scan {} marked as FAILED due to cancellation", scanId);
                handleCancellationFinal(scan);
            } else {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown internal error during scan";
                log.error("Scan failed for ID {}: {}", scanId, errorMsg, e);
                scan.setErrorMessage(errorMsg);
                updateScanStatus(scan, ScanStatus.FAILED);
                notificationService.notifyScanUpdate(scan, "Scan failed: " + errorMsg);
            }
        } finally {
            processRegistry.unregister(scanId);
            unlockScan(message);
        }
    }

    private void checkCancellation(Long scanId) {
        if (Thread.currentThread().isInterrupted() || processRegistry.isCancelled(scanId)) {
            throw new RuntimeException("Scan was cancelled by user");
        }
    }

    private void handleCancellationFinal(Scan scan) {
        scan.setStatus(ScanStatus.FAILED);
        scanRepository.save(scan);
        notificationService.notifyScanUpdate(scan, "Scan cancelled by user.");
    }

    private void updateScanStatus(Scan scan, ScanStatus status) {
        scan.setStatus(status);
        scanRepository.save(scan);
    }

    private void unlockScan(ScanMessage message) {
        // Implementation for distributed locks if added later
    }
}
