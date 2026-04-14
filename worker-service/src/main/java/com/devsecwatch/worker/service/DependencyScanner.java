package com.devsecwatch.worker.service;

import com.devsecwatch.worker.model.EnrichedFinding;
import com.devsecwatch.worker.model.Finding;
import com.devsecwatch.worker.model.enums.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DependencyScanner {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OSV_BATCH_URL = "https://api.osv.dev/v1/querybatch";

    public List<EnrichedFinding> scanDependencies(Path repoPath) {
        List<EnrichedFinding> allFindings = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(repoPath, 3)) { // Max depth 3 to avoid scanning deeply nested dependencies
            List<Path> manifestFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String pStr = p.toString();
                        return !pStr.contains("node_modules") 
                            && !pStr.contains(".git") 
                            && !pStr.contains("target");
                    })
                    .filter(p -> isManifestFile(p))
                    .collect(Collectors.toList());

            for (Path manifest : manifestFiles) {
                List<PackageQuery> queries = parseManifest(manifest);
                if (!queries.isEmpty()) {
                    allFindings.addAll(queryOsvAndMapFindings(queries, repoPath.relativize(manifest).toString()));
                }
            }
            
        } catch (IOException e) {
            log.error("Failed to walk repository for dependencies: {}", e.getMessage());
        }

        return allFindings;
    }

    private boolean isManifestFile(Path path) {
        String name = path.getFileName().toString();
        return "package.json".equals(name) || "pom.xml".equals(name) || "requirements.txt".equals(name);
    }

    private List<PackageQuery> parseManifest(Path manifestPath) {
        List<PackageQuery> queries = new ArrayList<>();
        String name = manifestPath.getFileName().toString();
        
        try {
            String content = Files.readString(manifestPath);
            
            if ("package.json".equals(name)) {
                JsonNode root = objectMapper.readTree(content);
                extractNpmDependencies(root.path("dependencies"), queries);
                extractNpmDependencies(root.path("devDependencies"), queries);
            } else if ("pom.xml".equals(name)) {
                // Regex to extract Maven dependencies
                Pattern p = Pattern.compile("(?s)<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]+)</version>\\s*</dependency>");
                Matcher m = p.matcher(content);
                while (m.find()) {
                    String groupId = m.group(1).trim();
                    String artifactId = m.group(2).trim();
                    String version = m.group(3).trim();
                    // OSV expects groupId:artifactId for Maven
                    if (!version.startsWith("$")) { // Skip properties
                        queries.add(new PackageQuery(groupId + ":" + artifactId, version, "Maven"));
                    }
                }
            } else if ("requirements.txt".equals(name)) {
                // simple pip dependencies
                Pattern p = Pattern.compile("^([a-zA-Z0-9\\-_]+)==([0-9\\.]+)", Pattern.MULTILINE);
                Matcher m = p.matcher(content);
                while (m.find()) {
                    queries.add(new PackageQuery(m.group(1), m.group(2), "PyPI"));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse manifest {}: {}", manifestPath, e.getMessage());
        }
        
        return queries;
    }

    private void extractNpmDependencies(JsonNode deps, List<PackageQuery> queries) {
        if (deps.isObject()) {
            deps.fieldNames().forEachRemaining(pkgName -> {
                String version = deps.path(pkgName).asText().replace("^", "").replace("~", "");
                queries.add(new PackageQuery(pkgName, version, "npm"));
            });
        }
    }

    private List<EnrichedFinding> queryOsvAndMapFindings(List<PackageQuery> queries, String filePath) {
        List<EnrichedFinding> findings = new ArrayList<>();
        if (queries.isEmpty()) return findings;

        // Construct OSV batch request
        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> queriesList = new ArrayList<>();
        
        for (PackageQuery pq : queries) {
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("version", pq.version);
            Map<String, String> pkgMap = new HashMap<>();
            pkgMap.put("name", pq.name);
            pkgMap.put("ecosystem", pq.ecosystem);
            queryMap.put("package", pkgMap);
            queriesList.add(queryMap);
        }
        
        requestBody.put("queries", queriesList);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            String responseStr = restTemplate.postForObject(OSV_BATCH_URL, entity, String.class);
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode results = root.path("results");
            
            if (results.isArray()) {
                for (int i = 0; i < results.size(); i++) {
                    JsonNode result = results.get(i);
                    JsonNode vulns = result.path("vulns");
                    
                    if (vulns.isArray() && vulns.size() > 0) {
                        PackageQuery pq = queries.get(i);
                        for (JsonNode vuln : vulns) {
                            findings.add(mapOsvToFinding(vuln, pq, filePath));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("OSV API batch query failed: {}", e.getMessage());
        }
        
        return findings;
    }

    private EnrichedFinding mapOsvToFinding(JsonNode vuln, PackageQuery pq, String filePath) {
        String description = vuln.path("summary").asText();
        if (description == null || description.isBlank()) {
            description = vuln.path("details").asText("No description available");
        }
        
        // Extract CVE
        String cveId = null;
        JsonNode aliases = vuln.path("aliases");
        if (aliases.isArray()) {
            for (JsonNode alias : aliases) {
                if (alias.asText().startsWith("CVE-")) {
                    cveId = alias.asText();
                    break;
                }
            }
        }
        
        // Extract fixed version
        String fixedVersion = "Update to a secure version";
        JsonNode affected = vuln.path("affected");
        if (affected.isArray() && affected.size() > 0) {
            JsonNode ranges = affected.get(0).path("ranges");
            if (ranges.isArray() && ranges.size() > 0) {
                JsonNode events = ranges.get(0).path("events");
                if (events.isArray()) {
                    for (JsonNode event : events) {
                        if (event.has("fixed")) {
                            fixedVersion = "Upgrade " + pq.name + " to " + event.path("fixed").asText();
                        }
                    }
                }
            }
        }

        Severity severity = Severity.HIGH; // Default
        
        // Try extracting score from database_specific if present.
        JsonNode databaseSpecific = vuln.path("database_specific");
        if (databaseSpecific.has("severity")) {
            String sev = databaseSpecific.path("severity").asText().toUpperCase();
            if (sev.contains("CRITICAL")) severity = Severity.CRITICAL;
            else if (sev.contains("HIGH")) severity = Severity.HIGH;
            else if (sev.contains("MODERATE") || sev.contains("MEDIUM")) severity = Severity.MEDIUM;
            else if (sev.contains("LOW")) severity = Severity.LOW;
        }
        
        Finding finding = Finding.builder()
                .vulnerabilityType("VULNERABLE_DEPENDENCY")
                .filePath(filePath)
                .lineNumber(0)
                .cveId(cveId)
                .severity(severity)
                .description(description)
                .codeSnippet(pq.name + "@" + pq.version)
                .ruleId(vuln.path("id").asText())
                .build();
                
        return EnrichedFinding.builder()
                .finding(finding)
                .explanation(com.devsecwatch.worker.dto.ai.AIExplanation.builder()
                        .description(description)
                        .fixSuggestion(fixedVersion)
                        .isTemplate(true)
                        .build())
                .build();
    }

    private static class PackageQuery {
        String name;
        String version;
        String ecosystem;
        
        PackageQuery(String name, String version, String ecosystem) {
            this.name = name;
            this.version = version;
            this.ecosystem = ecosystem;
        }
    }
}
