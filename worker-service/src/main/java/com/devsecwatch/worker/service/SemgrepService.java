package com.devsecwatch.worker.service;

import com.devsecwatch.worker.exception.SemgrepExecutionException;
import com.devsecwatch.worker.model.Finding;
import com.devsecwatch.worker.model.SemgrepResult;
import com.devsecwatch.worker.model.enums.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class SemgrepService {

    private final ObjectMapper objectMapper;

    public SemgrepResult runScan(Path repoPath) {
        log.info("Running Semgrep on {}", repoPath);

        Path jsonOutputPath = repoPath.resolve("semgrep_output.json");

        // Command: semgrep --config auto --json -o semgrep_output.json .
        ProcessBuilder processBuilder = new ProcessBuilder(
                "semgrep",
                "--config", "auto",
                "--json",
                "-o", "semgrep_output.json",
                ".");
        processBuilder.directory(repoPath.toFile());
        // Do NOT redirect error stream to stdout, keep them separate to avoid polluting
        // JSON
        processBuilder.redirectErrorStream(false);

        // Set UTF-8 encoding variables
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");
        processBuilder.environment().put("PYTHONUTF8", "1");
        processBuilder.environment().put("LC_ALL", "en_US.UTF-8");

        long startTime = System.currentTimeMillis();
        Process process = null;
        try {
            process = processBuilder.start();

            // Consume stdout and stderr streams to prevent blocking
            consumeStream(process.getInputStream());
            consumeStream(process.getErrorStream());

            boolean finished = process.waitFor(180, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new SemgrepExecutionException("Semgrep timeout (>180s)");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // Check if file exists anyway, sometimes non-zero exit code generates output
                if (!jsonOutputPath.toFile().exists()) {
                    throw new SemgrepExecutionException("Semgrep failed with exit code: " + exitCode);
                }
            }

            if (!jsonOutputPath.toFile().exists()) {
                // Fallback: try capturing stdout if file wasn't created (shouldn't happen with
                // -o)
                throw new SemgrepExecutionException("Semgrep passed but no output file created");
            }

            // Read the JSON file content
            byte[] bytes = java.nio.file.Files.readAllBytes(jsonOutputPath);
            String jsonOutput = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            return parseOutput(jsonOutput, System.currentTimeMillis() - startTime);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new SemgrepExecutionException("Semgrep execution failed: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            // Cleanup temp file
            try {
                java.nio.file.Files.deleteIfExists(jsonOutputPath);
            } catch (IOException ignored) {
            }
        }
    }

    private void consumeStream(java.io.InputStream stream) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                while (reader.readLine() != null) {
                    // Just consume loop
                }
            } catch (IOException ignored) {
            }
        }).start();
    }

    private SemgrepResult parseOutput(String jsonOutput, long durationMs) {
        List<Finding> findings = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonOutput);
            JsonNode results = root.path("results");

            if (results.isArray()) {
                for (JsonNode node : results) {
                    Severity severity = mapSeverity(node.path("extra").path("severity").asText());

                    // Filter: Only keep HIGH and CRITICAL
                    if (severity == Severity.HIGH || severity == Severity.CRITICAL) {
                        Finding finding = Finding.builder()
                                .ruleId(node.path("check_id").asText())
                                .filePath(node.path("path").asText())
                                .lineNumber(node.path("start").path("line").asInt())
                                .severity(severity)
                                .description(node.path("extra").path("message").asText())
                                .codeSnippet(node.path("extra").path("lines").asText()) // Simple extraction
                                .vulnerabilityType(node.path("check_id").asText()) // Using rule ID as type for now
                                .semgrepConfidence(0.8) // Placeholder
                                .cveId(node.path("extra").path("metadata").path("cve").asText(null))
                                .build();
                        findings.add(finding);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Semgrep output: {}", e.getMessage());
            // Might allow empty results if parsing fails, or throw exception.
            // Better to throw.
            throw new SemgrepExecutionException("Failed to parse Semgrep output");
        }

        log.info("Semgrep found {} HIGH/CRITICAL findings", findings.size());
        return SemgrepResult.builder()
                .findings(findings)
                .totalFindings(findings.size())
                .executionTimeMs(durationMs)
                .build();
    }

    private Severity mapSeverity(String semgrepSeverity) {
        if (semgrepSeverity == null)
            return Severity.LOW;
        switch (semgrepSeverity.toUpperCase()) {
            case "ERROR":
                return Severity.CRITICAL; // Semgrep generic "ERROR" often maps to High/Critical
            case "WARNING":
                return Severity.HIGH;
            case "INFO":
                return Severity.LOW;
            default:
                return Severity.MEDIUM;
        }
    }
}
