package com.devsecwatch.backend.service;

import com.devsecwatch.backend.dto.message.ScanMessage;
import com.devsecwatch.backend.dto.scan.ScanRequest;
import com.devsecwatch.backend.dto.scan.ScanResponse;
import com.devsecwatch.backend.dto.scan.VulnerabilityResponse;
import com.devsecwatch.backend.exception.*;
import com.devsecwatch.backend.mapper.ScanMapper;
import com.devsecwatch.backend.model.Scan;
import com.devsecwatch.backend.model.User;
import com.devsecwatch.backend.model.enums.ScanStatus;
import com.devsecwatch.backend.repository.ScanRepository;
import com.devsecwatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScanService {

    private final ScanRepository scanRepository;
    private final UserRepository userRepository;
    private final MessagePublisherService messagePublisherService;
    private final GitService gitService;
    private final ScanMapper scanMapper;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final RateLimiterService rateLimiterService;

    private static final String SCAN_LOCK_KEY_PREFIX = "scanning:";

    @Transactional
    public ScanResponse createScan(ScanRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        // 0. Rate Limiting Check
        io.github.bucket4j.ConsumptionProbe probe = rateLimiterService.tryConsumeAndReturnProbe(username);
        if (probe != null && !probe.isConsumed()) {
            log.warn("Rate limit exceeded for user: {}", username);
            int remainingSeconds = (int) (probe.getNanosToWaitForRefill() / 1_000_000_000L);
            throw new RateLimitExceededException(
                    "You have reached the scan limit (5 scans per 10 minutes). Please wait " + remainingSeconds + " seconds before trying again.", remainingSeconds);
        }

        // 1. Deduplication Check
        String lockKey = SCAN_LOCK_KEY_PREFIX + request.getRepoUrl() + ":" + user.getId();
        Object existingScanId = redisTemplate.opsForValue().get(lockKey);

        if (existingScanId != null) {
            log.info("Scan already in progress for repository: {} and user: {}. Returning existing scan ID: {}", 
                    request.getRepoUrl(), username, existingScanId);
            Long id = Long.valueOf(existingScanId.toString());
            Scan scan = scanRepository.findById(id)
                    .orElseThrow(() -> new ScanNotFoundException("Existing scan not found in database: " + id));
            return scanMapper.toResponse(scan);
        }

        log.info("Creating scan for repository: {} by user: {}", request.getRepoUrl(), username);

        if (!gitService.isRepositoryAccessible(request.getRepoUrl())) {
            throw new RepositoryNotAccessibleException("Repository is private, doesn't exist, or unreachable");
        }

        Scan scan = Scan.builder()
                .user(user)
                .repoUrl(request.getRepoUrl())
                .branch(request.getBranch())
                .status(ScanStatus.QUEUED)
                .totalFiles(0)
                .linesOfCode(0)
                .totalVulnerabilities(0)
                .criticalCount(0)
                .highCount(0)
                .mediumCount(0)
                .lowCount(0)
                .build();

        scan = scanRepository.save(scan);
        log.info("Scan created with ID: {}", scan.getId());

        // Set lock in Redis with 15-minute TTL
        redisTemplate.opsForValue().set(lockKey, scan.getId(), java.time.Duration.ofMinutes(15));

        ScanMessage message = ScanMessage.builder()
                .scanId(scan.getId())
                .userId(user.getId())
                .repoUrl(scan.getRepoUrl())
                .branch(scan.getBranch())
                .correlationId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            // Register synchronization to publish message AFTER transaction commit
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                messagePublisherService.publishScanJob(message);
                                log.info("Message published for scan ID: {}", message.getScanId());
                            } catch (MessagePublishException e) {
                                log.error("Failed to publish scan message after commit for scan ID: {}",
                                        message.getScanId(), e);
                            }
                        }
                    });
            log.info("Registered scan message publication for after commit, scan ID: {}", scan.getId());
        } catch (Exception e) {
            log.warn("Could not register transaction synchronization, publishing immediately", e);
            messagePublisherService.publishScanJob(message);
        }

        return scanMapper.toResponse(scan);
    }

    @Transactional(readOnly = true)
    public ScanResponse getScanById(Long scanId, String username) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ScanNotFoundException("Scan not found with ID: " + scanId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (!scan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Cannot access another user's scan");
        }

        return scanMapper.toResponse(scan);
    }

    @Transactional(readOnly = true)
    public List<VulnerabilityResponse> getVulnerabilitiesByScanId(Long scanId, String username) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ScanNotFoundException("Scan not found with ID: " + scanId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (!scan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Cannot access another user's scan");
        }

        ScanResponse response = scanMapper.toResponse(scan);
        return response.getVulnerabilities();
    }

    @Transactional(readOnly = true)
    public Page<ScanResponse> getUserScans(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        return scanRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(scanMapper::toResponse);
    }

    @Transactional
    public void deleteScan(Long scanId, String username) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ScanNotFoundException("Scan not found with ID: " + scanId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (!scan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Cannot delete another user's scan");
        }

        if (scan.getStatus() != ScanStatus.FAILED && scan.getStatus() != ScanStatus.QUEUED) {
            throw new IllegalStateException("Cannot delete scan in " + scan.getStatus() + " status");
        }

        scanRepository.delete(scan);
        log.info("Deleted scan ID: {} by user: {}", scanId, username);
    }

    @Transactional
    public void cancelScan(Long scanId, String username) {
        Scan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new ScanNotFoundException("Scan not found with ID: " + scanId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        if (!scan.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Cannot cancel another user's scan");
        }

        if (scan.getStatus() != ScanStatus.QUEUED && scan.getStatus() != ScanStatus.PROCESSING) {
            throw new IllegalStateException("Cannot cancel scan in " + scan.getStatus() + " status");
        }

        log.info("Cancelling scan ID: {} by user: {}", scanId, username);
        scan.setStatus(ScanStatus.FAILED);
        scan.setErrorMessage("Scan cancelled by user");
        scan.setCompletedAt(LocalDateTime.now());
        scanRepository.save(scan);

        com.devsecwatch.backend.dto.message.ScanCancellationMessage message = 
            com.devsecwatch.backend.dto.message.ScanCancellationMessage.builder()
                .scanId(scanId)
                .username(username)
                .correlationId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .build();

        messagePublisherService.publishScanCancellation(message);
    }
}
