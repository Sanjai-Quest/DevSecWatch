package com.devsecwatch.backend.service.impl;

import com.devsecwatch.backend.dto.chat.*;
import com.devsecwatch.backend.model.Scan;
import com.devsecwatch.backend.model.User;
import com.devsecwatch.backend.model.Vulnerability;
import com.devsecwatch.backend.repository.ScanRepository;
import com.devsecwatch.backend.service.AiServiceClient;
import com.devsecwatch.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final AiServiceClient aiServiceClient;
    private final ScanRepository scanRepository;

    @Override
    public ChatResponse processMessage(ChatRequest request, User user) {
        log.info("Processing chat message for user: {}", user.getUsername());

        // Build context from user's scans
        ChatContext context = null;
        if (request.getIncludeContext() != null && request.getIncludeContext()) {
            context = buildUserContext(user, request.getScanId());
        }

        // Generate AI response
        String aiResponse = aiServiceClient.generateChatResponse(
                request.getMessage(),
                request.getConversationHistory(),
                context);

        // Generate suggested follow-up questions
        List<String> suggestions = generateSuggestedQuestions(request.getMessage(), context);

        return ChatResponse.builder()
                .response(aiResponse)
                .messageId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .suggestedQuestions(suggestions)
                .model("llama-3.3-70b-versatile")
                .build();
    }

    private ChatContext buildUserContext(User user, Long specificScanId) {
        try {
            if (specificScanId != null) {
                // Build context for specific scan
                return buildScanSpecificContext(specificScanId, user);
            } else {
                // Build general user context
                return buildGeneralContext(user);
            }
        } catch (Exception e) {
            log.error("Error building chat context", e);
            return null;
        }
    }

    private ChatContext buildScanSpecificContext(Long scanId, User user) {
        Optional<Scan> scanOpt = scanRepository.findById(scanId);

        if (scanOpt.isEmpty() || !scanOpt.get().getUser().getId().equals(user.getId())) {
            return null;
        }

        Scan scan = scanOpt.get();

        Map<String, Integer> severityCounts = new HashMap<>();
        severityCounts.put("Critical", scan.getCriticalCount() != null ? scan.getCriticalCount() : 0);
        severityCounts.put("High", scan.getHighCount() != null ? scan.getHighCount() : 0);
        severityCounts.put("Medium", scan.getMediumCount() != null ? scan.getMediumCount() : 0);
        severityCounts.put("Low", scan.getLowCount() != null ? scan.getLowCount() : 0);

        List<String> vulnTypes = new ArrayList<>();
        if (scan.getVulnerabilities() != null) {
            vulnTypes = scan.getVulnerabilities().stream()
                    .map(Vulnerability::getVulnerabilityType)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        String summary = String.format(
                "Scan of %s completed on %s. Found %d vulnerabilities.",
                scan.getRepoUrl(),
                scan.getCreatedAt().toLocalDate(),
                scan.getTotalVulnerabilities() != null ? scan.getTotalVulnerabilities() : 0);

        return ChatContext.builder()
                .totalScans(1)
                .totalVulnerabilities(scan.getTotalVulnerabilities() != null ? scan.getTotalVulnerabilities() : 0)
                .severityCounts(severityCounts)
                .topVulnerabilityTypes(vulnTypes)
                .repositories(List.of(scan.getRepoUrl()))
                .recentScanSummary(summary)
                .build();
    }

    private ChatContext buildGeneralContext(User user) {
        List<Scan> recentScans = scanRepository.findByUserOrderByCreatedAtDesc(
                user,
                PageRequest.of(0, 10)).getContent();

        int totalScans = recentScans.size();
        int totalVulns = recentScans.stream()
                .mapToInt(s -> s.getTotalVulnerabilities() != null ? s.getTotalVulnerabilities() : 0)
                .sum();

        Map<String, Integer> severityCounts = new HashMap<>();
        severityCounts.put("Critical", recentScans.stream()
                .mapToInt(s -> s.getCriticalCount() != null ? s.getCriticalCount() : 0).sum());
        severityCounts.put("High", recentScans.stream()
                .mapToInt(s -> s.getHighCount() != null ? s.getHighCount() : 0).sum());
        severityCounts.put("Medium", recentScans.stream()
                .mapToInt(s -> s.getMediumCount() != null ? s.getMediumCount() : 0).sum());
        severityCounts.put("Low", recentScans.stream()
                .mapToInt(s -> s.getLowCount() != null ? s.getLowCount() : 0).sum());

        Map<String, Long> vulnTypeCounts = recentScans.stream()
                .filter(s -> s.getVulnerabilities() != null)
                .flatMap(s -> s.getVulnerabilities().stream())
                .filter(v -> v.getVulnerabilityType() != null)
                .collect(Collectors.groupingBy(Vulnerability::getVulnerabilityType, Collectors.counting()));

        List<String> topTypes = vulnTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> repos = recentScans.stream()
                .map(Scan::getRepoUrl)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        String summary = String.format(
                "User has performed %d scans with a total of %d vulnerabilities discovered.",
                totalScans, totalVulns);

        return ChatContext.builder()
                .totalScans(totalScans)
                .totalVulnerabilities(totalVulns)
                .severityCounts(severityCounts)
                .topVulnerabilityTypes(topTypes)
                .repositories(repos)
                .recentScanSummary(summary)
                .build();
    }

    private List<String> generateSuggestedQuestions(String userMessage, ChatContext context) {
        List<String> suggestions = new ArrayList<>();

        // Dynamic suggestions based on context
        if (context != null) {
            if (context.getSeverityCounts().getOrDefault("Critical", 0) > 0) {
                suggestions.add("How do I fix critical vulnerabilities?");
            }
            if (!context.getTopVulnerabilityTypes().isEmpty()) {
                String topType = context.getTopVulnerabilityTypes().get(0);
                suggestions.add("Tell me more about " + topType + " vulnerabilities");
            }
        }

        // Static suggestions
        suggestions.add("What are security best practices for my tech stack?");
        suggestions.add("How can I prevent SQL injection attacks?");
        suggestions.add("Explain the OWASP Top 10");

        return suggestions.stream().limit(3).collect(Collectors.toList());
    }
}
