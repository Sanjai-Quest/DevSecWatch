package com.devsecwatch.worker.service;

import com.devsecwatch.worker.exception.ScanExecutionException;
import com.devsecwatch.worker.exception.OutOfMemoryException;
import com.devsecwatch.worker.exception.SemgrepExecutionException;
import com.devsecwatch.worker.model.Finding;
import com.devsecwatch.worker.model.SemgrepResult;
import com.devsecwatch.worker.model.enums.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SemgrepService {

    private static final Logger log = LoggerFactory.getLogger(SemgrepService.class);
    private final ObjectMapper objectMapper;
    private final ScanProcessRegistry processRegistry;

    public SemgrepService(ObjectMapper objectMapper, ScanProcessRegistry processRegistry) {
        this.objectMapper = objectMapper;
        this.processRegistry = processRegistry;
    }

    public SemgrepResult runScan(Path repoPath, Long scanId) {
        log.info("Running Semgrep in Docker on {}", repoPath);
        String containerName = "semgrep-scan-" + scanId;
        Path customRulesPath = repoPath.resolve(".devsecwatch_secrets.yml");

        // Copy custom rules to repo path for semgrep to access
        try {
            java.io.InputStream is = getClass().getResourceAsStream("/semgrep-rules/secrets.yml");
            if (is != null) {
                java.nio.file.Files.copy(is, customRulesPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Could not copy custom semgrep rules: {}", e.getMessage());
        }

        boolean useDocker = System.getenv("USE_DOCKER_SEMGREP") != null && System.getenv("USE_DOCKER_SEMGREP").equals("true");
        List<String> cmd = new ArrayList<>();
        
        if (useDocker) {
            cmd.addAll(java.util.Arrays.asList(
                    "docker", "run", "--name", containerName, "--rm", "--read-only", "--tmpfs", "/tmp",
                    "--memory=512m", "--cpus=0.5", "--network=none", 
                    "--pids-limit=50", "--cap-drop=ALL", "--security-opt=no-new-privileges", "--user=1000:1000",
                    "-v", repoPath.toAbsolutePath().toString() + ":/src:ro",
                    "--workdir", "/src",
                    "returntocorp/semgrep:latest",
                    "semgrep", "scan", "--json", "--config=auto"
            ));
            if (customRulesPath.toFile().exists()) {
                cmd.add("--config=/src/.devsecwatch_secrets.yml");
            }
            cmd.add("/src");
        } else {
            // Native semgrep execution
            cmd.addAll(java.util.Arrays.asList(
                "semgrep", "scan", "--json", "--config=auto", "--quiet"
            ));
            if (customRulesPath.toFile().exists()) {
                cmd.add("--config=" + customRulesPath.toAbsolutePath().toString());
            }
            cmd.add(repoPath.toAbsolutePath().toString());
        }

        ProcessBuilder processBuilder = createProcessBuilder(cmd);
        processBuilder.redirectErrorStream(false);

        long startTime = System.currentTimeMillis();
        Process process = null;
        try {
            process = processBuilder.start();
            processRegistry.registerProcess(scanId, process);
            
            final Process activeProcess = process;

            // Consume stderr to prevent blocking
            consumeStream(activeProcess.getErrorStream());
            
            // Read stdout asynchronously
            java.util.concurrent.CompletableFuture<String> stdoutFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(activeProcess.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read stdout", e);
                }
            });

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ScanExecutionException("Semgrep timeout (>300s)");
            }

            int exitCode = process.exitValue();
            if (exitCode == 137) {
                throw new OutOfMemoryException("Repository scan breached 512MB limit.");
            }
            if (exitCode != 0 && exitCode != 1) {
                throw new ScanExecutionException("Semgrep failed with exit code: " + exitCode);
            }

            String jsonOutput = stdoutFuture.join();
            if (jsonOutput == null || jsonOutput.trim().isEmpty()) {
                throw new ScanExecutionException("Semgrep produced no output on stdout");
            }

            return parseOutput(jsonOutput, System.currentTimeMillis() - startTime);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ScanExecutionException("Semgrep execution error: " + e.getMessage(), e);
        } catch (java.util.concurrent.CompletionException e) {
            throw new ScanExecutionException("Error reading Semgrep stdout", e.getCause());
        } finally {
            if (process != null) {
                processRegistry.unregisterProcess(scanId, process);
                if (process.isAlive()) process.destroyForcibly();
            }
            forceCleanupContainer(containerName);
            try { java.nio.file.Files.deleteIfExists(customRulesPath); } catch (IOException ignored) {}
        }
    }

    protected ProcessBuilder createProcessBuilder(List<String> cmd) {
        return new ProcessBuilder(cmd);
    }

    private void forceCleanupContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "rm", "-f", containerName).start().waitFor();
            log.info("Force killed container: {}", containerName);
        } catch (Exception e) {
            log.error("Failed to cleanup container {}", containerName, e);
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
                    String ruleId = node.path("check_id").asText();
                    String ruleIdLower = ruleId.toLowerCase();
                    Severity severity;
                    
                    if (ruleIdLower.contains("secret") || ruleIdLower.contains("credential") || ruleIdLower.contains("key")) {
                        severity = Severity.HIGH;
                    } else {
                        severity = mapSeverity(node.path("extra").path("severity").asText());
                    }

                    // Filter: Only keep HIGH and CRITICAL
                    if (severity == Severity.HIGH || severity == Severity.CRITICAL) {
                        Finding finding = Finding.builder()
                                .ruleId(ruleId)
                                .filePath(node.path("path").asText())
                                .lineNumber(node.path("start").path("line").asInt())
                                .severity(severity)
                                .description(node.path("extra").path("message").asText())
                                .codeSnippet(node.path("extra").path("lines").asText()) // Simple extraction
                                .vulnerabilityType(ruleId) // Using rule ID as type for now
                                .semgrepConfidence(0.8) // Placeholder
                                .cveId(node.path("extra").path("metadata").path("cve").asText(null))
                                .cweId(extractCwe(node.path("extra").path("metadata").path("cwe")))
                                .build();
                        findings.add(finding);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Semgrep output: {}", e.getMessage());
            throw new SemgrepExecutionException("Failed to parse Semgrep output");
        }

        log.info("Semgrep found {} HIGH/CRITICAL findings", findings.size());
        return SemgrepResult.builder()
                .findings(findings)
                .totalFindings(findings.size())
                .executionTimeMs(durationMs)
                .build();
    }

    private String extractCwe(JsonNode cweNode) {
        if (cweNode == null || cweNode.isNull() || cweNode.isMissingNode()) return null;
        if (cweNode.isArray() && cweNode.size() > 0) return cweNode.get(0).asText();
        return cweNode.asText();
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
