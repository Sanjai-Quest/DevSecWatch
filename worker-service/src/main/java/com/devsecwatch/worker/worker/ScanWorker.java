package com.devsecwatch.worker.worker;

import com.devsecwatch.worker.dto.message.ScanMessage;
import com.devsecwatch.worker.exception.GitCloneTimeoutException;
import com.devsecwatch.worker.exception.NoFilesFoundException;
import com.devsecwatch.worker.exception.SemgrepExecutionException;
import com.devsecwatch.worker.model.EnrichedFinding;
import com.devsecwatch.worker.model.Scan;
import com.devsecwatch.worker.model.ScanMetrics;
import com.devsecwatch.worker.model.SemgrepResult;
import com.devsecwatch.worker.model.Vulnerability;
import com.devsecwatch.worker.model.enums.ConfidenceLevel;
import com.devsecwatch.worker.model.enums.ScanStatus;
import com.devsecwatch.worker.repository.ScanRepository;
import com.devsecwatch.worker.service.*;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ScanWorker {

    private final ScanRepository scanRepository;
    private final GitService gitService;
    private final FileService fileService;
    private final SemgrepService semgrepService;
    private final AIEnrichmentService aiEnrichmentService;
    private final ResultPersistenceService resultPersistenceService;
    private final WebSocketNotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.queue.scans}", ackMode = "MANUAL", concurrency = "1-3")
    @Transactional
    public void processScan(@Payload ScanMessage message, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        Long scanId = message.getScanId();
        String correlationId = message.getCorrelationId();

        log.info("Processing scan ID: {} with correlation ID: {}", scanId, correlationId);

        Scan scan = null;
        Path repoPath = null;
        long startTime = System.currentTimeMillis();
        long gitDuration = 0;
        long semgrepDuration = 0;
        long aiDuration = 0;

        try {
            // Fetch scan with User if possible to avoid lazy initialization errors later
            // Since we don't have a custom query yet, we rely on standard find methods.
            // If lazy loading fails, we might need @Transactional on this method.
            scan = scanRepository.findById(scanId).orElse(null);

            if (scan == null) {
                log.error("Scan ID {} not found in database. Acking message.", scanId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            if (scan.getStatus() == ScanStatus.COMPLETED || scan.getStatus() == ScanStatus.FAILED) {
                log.warn("Scan ID {} already processed.", scanId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            updateScanStatus(scan, ScanStatus.PROCESSING, null);

            // 1. Clone Repository
            long gitStart = System.currentTimeMillis();
            repoPath = gitService.cloneRepository(message.getRepoUrl(), message.getBranch(), scanId);
            gitDuration = System.currentTimeMillis() - gitStart;

            // 2. Extract & Count Files
            List<Path> javaFiles = fileService.extractJavaFiles(repoPath);
            // Note: extractJavaFiles returns List<Path>, capture wildcard or cast if
            // needed, or use var/List<Path>
            // Assuming extractJavaFiles signature follows previous step which returns
            // List<Path>
            int loc = fileService.countLinesOfCode(javaFiles);

            scan.setTotalFiles(javaFiles.size());
            scan.setLinesOfCode(loc);
            scanRepository.save(scan);

            // 3. Run Semgrep Analysis
            long semgrepStart = System.currentTimeMillis();
            SemgrepResult result = semgrepService.runScan(repoPath);
            semgrepDuration = System.currentTimeMillis() - semgrepStart;

            // 4. AI Enrichment
            long aiStart = System.currentTimeMillis();
            List<EnrichedFinding> enrichedFindings = aiEnrichmentService.enrichFindings(result.getFindings());
            aiDuration = System.currentTimeMillis() - aiStart;

            // Calculate Metrics
            // Assuming cache hit rate is tracked in AI service logs or we calculate here
            // For now, let's calc raw if needed or just pass 0 if not exposed.
            // aiEnrichmentService returns enriched list. We can't easily see hits here
            // without exposing it.
            // We'll calculate roughly: any template explanation might be a "miss" if
            // intended?
            // Actually, templates are fallbacks. Hits are real AI or Redis.
            // Let's assume passed in Metrics object.
            // Better: update AI service to return a result object with stats?
            // For now, use BigDecimal.ZERO as placeholder or update Service signature
            // later.

            ScanMetrics metrics = ScanMetrics.builder()
                    .scan(scan)
                    .filesScanned(javaFiles.size())
                    .linesOfCode(loc)
                    .gitCloneDurationMs(gitDuration)
                    .semgrepDurationMs(semgrepDuration)
                    .aiCallDurationMs(aiDuration)
                    .totalDurationMs(System.currentTimeMillis() - startTime)
                    .apiCallsMade(enrichedFindings.size()) // Upper bound
                    .cacheHitRate(BigDecimal.ZERO) // Todo: expose from service
                    .build();

            // 5. Persist Results
            resultPersistenceService.saveScanResults(scan, enrichedFindings, metrics);

            // Send WebSocket notification
            try {
                notificationService.notifyScanComplete(scan);
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for scan {}", scan.getId(), e);
            }

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            handleFailure(scan, "Error processing scan: " + e.getMessage(), e);
            try {
                // Requeue only if not a known fatal error type
                boolean requeue = !(e instanceof com.devsecwatch.worker.exception.GitCloneException ||
                        e instanceof GitCloneTimeoutException ||
                        e instanceof NoFilesFoundException);
                channel.basicNack(deliveryTag, false, requeue);
            } catch (IOException ex) {
                log.error("Failed to NACK", ex);
            }
        } finally {
            if (repoPath != null) {
                gitService.cleanup(repoPath);
            }
        }
    }

    private void updateScanStatus(Scan scan, ScanStatus status, String errorMessage) {
        if (scan == null)
            return;
        scan.setStatus(status);
        if (status == ScanStatus.PROCESSING) {
            scan.setStartedAt(LocalDateTime.now());
        }
        if (errorMessage != null) {
            scan.setErrorMessage(errorMessage);
        }
        scanRepository.save(scan);
    }

    private void completeScan(Scan scan) {
        if (scan == null)
            return;
        scan.setStatus(ScanStatus.COMPLETED);
        scan.setCompletedAt(LocalDateTime.now());
        scanRepository.save(scan);
    }

    private void handleFailure(Scan scan, String reason, Exception e) {
        log.error("Scan processing failed: {}", reason, e);
        if (scan != null) {
            scan.setStatus(ScanStatus.FAILED);
            scan.setErrorMessage(reason);
            scan.setCompletedAt(LocalDateTime.now());
            scanRepository.save(scan);
            try {
                notificationService.notifyScanComplete(scan);
            } catch (Exception ex) {
                log.error("Failed to send failure notification", ex);
            }
        }
    }

    private void nack(Channel channel, long tag, boolean requeue) {
        try {
            channel.basicNack(tag, false, requeue);
        } catch (IOException ex) {
            log.error("Failed to NACK message", ex);
        }
    }
}
