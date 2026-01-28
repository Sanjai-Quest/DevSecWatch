package com.devsecwatch.backend.controller;

import com.devsecwatch.backend.dto.chat.ChatRequest;
import com.devsecwatch.backend.dto.chat.ChatResponse;
import com.devsecwatch.backend.model.User;
import com.devsecwatch.backend.repository.UserRepository;
import com.devsecwatch.backend.service.ChatService;
import com.devsecwatch.backend.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;
    private final RateLimitService rateLimitService;
    private final UserRepository userRepository;

    @GetMapping("/test-auth")
    public ResponseEntity<?> testAuth() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", auth != null && auth.isAuthenticated());
            response.put("username", auth != null ? auth.getName() : "null");
            response.put("authorities", auth != null ? auth.getAuthorities().toString() : "null");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatRequest request) {

        try {
            log.info("=== CHAT REQUEST START ===");

            // Get authentication
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("Authentication: {}", authentication);

            if (authentication == null) {
                log.error("Authentication is null");
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            if (!authentication.isAuthenticated()) {
                log.error("User not authenticated");
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String username = authentication.getName();
            log.info("Username from auth: {}", username);

            if (username == null || username.equals("anonymousUser")) {
                log.error("Anonymous user");
                return ResponseEntity.status(401).body(Map.of("error", "Anonymous user"));
            }

            // Load user
            log.info("Loading user from database...");
            User user = userRepository.findByUsername(username).orElse(null);

            if (user == null) {
                log.error("User not found in database: {}", username);
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            log.info("User found: {} (ID: {})", user.getUsername(), user.getId());

            // Rate limit check
            if (!rateLimitService.allowRequest(user.getUsername())) {
                int remaining = rateLimitService.getRemainingRequests(user.getUsername());
                log.warn("Rate limit exceeded");
                return ResponseEntity.status(429).body(Map.of(
                        "error", "Rate limit exceeded",
                        "remainingRequests", remaining));
            }

            // Process message
            log.info("Processing message: {}",
                    request.getMessage().substring(0, Math.min(50, request.getMessage().length())));
            ChatResponse response = chatService.processMessage(request, user);
            log.info("Response generated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("=== CHAT ERROR ===");
            log.error("Exception: {}", e.getClass().getName());
            log.error("Message: {}", e.getMessage());
            log.error("Stack trace:", e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            errorResponse.put("type", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chat service is running");
    }
}
