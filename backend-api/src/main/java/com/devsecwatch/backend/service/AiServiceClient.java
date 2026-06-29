package com.devsecwatch.backend.service;

import com.devsecwatch.backend.dto.chat.ChatContext;
import com.devsecwatch.backend.dto.chat.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public String generateChatResponse(String userMessage, List<ChatMessage> history, ChatContext context) {
        try {
            String systemPrompt = buildSystemPrompt(context);
            List<Map<String, String>> messages = buildMessages(userMessage, history);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", userMessage);
            requestBody.put("history", messages);
            requestBody.put("context", systemPrompt);

            log.info("Sending chat request to AI service at: {}", aiServiceUrl);

            WebClient webClient = webClientBuilder
                    .baseUrl(aiServiceUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String responseBody = webClient.post()
                    .uri("/chat")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseResponse(responseBody);

        } catch (Exception e) {
            log.error("Error calling AI service", e);
            return "I apologize, but I encountered an error processing your request. Please try again.";
        }
    }

    private String buildSystemPrompt(ChatContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a security expert AI assistant integrated into DevSecWatch, ");
        prompt.append("a security scanning platform. Your role is to help developers understand ");
        prompt.append("vulnerabilities, provide remediation guidance, and answer security questions.\n\n");

        prompt.append("Key Guidelines:\n");
        prompt.append("- Provide clear, actionable security advice\n");
        prompt.append("- Include code examples when relevant\n");
        prompt.append("- Explain concepts in simple terms\n");
        prompt.append("- Prioritize based on severity (Critical > High > Medium > Low)\n");
        prompt.append("- Be concise but thorough\n");
        prompt.append("- Always recommend secure coding practices\n\n");

        if (context != null) {
            prompt.append("User's Security Context:\n");
            prompt.append(String.format("- Total Scans: %d\n", context.getTotalScans()));
            prompt.append(String.format("- Total Vulnerabilities: %d\n", context.getTotalVulnerabilities()));

            if (context.getSeverityCounts() != null) {
                prompt.append("- Severity Breakdown:\n");
                context.getSeverityCounts()
                        .forEach((severity, count) -> prompt.append(String.format("  * %s: %d\n", severity, count)));
            }

            if (context.getTopVulnerabilityTypes() != null && !context.getTopVulnerabilityTypes().isEmpty()) {
                prompt.append("- Common Vulnerabilities: ");
                prompt.append(String.join(", ", context.getTopVulnerabilityTypes()));
                prompt.append("\n");
            }

            if (context.getRecentScanSummary() != null) {
                prompt.append("\nRecent Scan Summary:\n");
                prompt.append(context.getRecentScanSummary());
                prompt.append("\n");
            }
        }

        return prompt.toString();
    }

    private List<Map<String, String>> buildMessages(String userMessage, List<ChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();

        // Add conversation history (limit to last 10 messages to avoid token limits)
        if (history != null && !history.isEmpty()) {
            int startIndex = Math.max(0, history.size() - 10);
            for (int i = startIndex; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                Map<String, String> message = new HashMap<>();
                message.put("role", msg.getRole());
                message.put("content", msg.getContent());
                messages.add(message);
            }
        }

        return messages;
    }

    private String parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String response = root.get("response").asText();
            return response;
        } catch (Exception e) {
            log.error("Error parsing AI service response", e);
            return "Unable to parse AI response.";
        }
    }
}
