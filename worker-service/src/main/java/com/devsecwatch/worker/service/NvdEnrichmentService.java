package com.devsecwatch.worker.service;

import com.devsecwatch.worker.model.EnrichedFinding;
import com.devsecwatch.worker.model.Finding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NvdEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(NvdEnrichmentService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public NvdEnrichmentService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<EnrichedFinding> enrichWithNvdInfo(List<EnrichedFinding> findings) {
        for (EnrichedFinding enrichedFinding : findings) {
            Finding finding = enrichedFinding.getFinding();
            String cweId = finding.getCweId();
            String cveId = finding.getCveId();

            try {
                if (cweId != null && !cweId.isBlank()) {
                    String parsedCwe = extractPattern(cweId, "CWE-\\d+");
                    if (parsedCwe != null) {
                        enrichSingleFinding(enrichedFinding, parsedCwe, "cweId");
                    }
                } else {
                    log.debug("No CWE ID found for finding: {}", finding.getVulnerabilityType());
                }
                
                // If we have a CVE ID, prioritize it for precise enrichment 
                if (cveId != null && !cveId.isBlank()) {
                    String parsedCve = extractPattern(cveId, "CVE-\\d{4}-\\d+");
                    if (parsedCve != null) {
                        enrichSingleFinding(enrichedFinding, parsedCve.toUpperCase(), "cveId");
                    }
                } else {
                    log.debug("No CVE ID found for finding: {}", finding.getVulnerabilityType());
                }
            } catch (Exception e) {
                log.warn("Failed to enrich finding: {}", e.getMessage());
            }
        }
        return findings;
    }

    private String extractPattern(String raw, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }

    private void enrichSingleFinding(EnrichedFinding enrichedFinding, String id, String idParamName) {
        String redisKey = "nvd:" + idParamName + ":" + id;
        String cachedResult = redisTemplate.opsForValue().get(redisKey);

        if (cachedResult != null) {
            log.debug("NVD Cache hit for {}={}", idParamName, id);
            applyNvdData(enrichedFinding, cachedResult);
            return;
        }

        log.info("Fetching NVD data for {}={}", idParamName, id);
        String url = "https://services.nvd.nist.gov/rest/json/cves/2.0?" + idParamName + "=" + id + "&resultsPerPage=1";
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response != null && response.contains("vulnerabilities")) {
                JsonNode root = objectMapper.readTree(response);
                int totalResults = root.path("totalResults").asInt(0);
                if (totalResults > 0) {
                    // Cache for 24 hours to avoid rate limiting
                    redisTemplate.opsForValue().set(redisKey, response, 24, TimeUnit.HOURS);
                    applyNvdData(enrichedFinding, response);
                } else {
                    log.warn("NVD API returned 0 results for {}={}", idParamName, id);
                }
            } else {
                log.warn("NVD API returned empty or invalid response for {}={}", idParamName, id);
            }
        } catch (Exception e) {
            log.error("API error when calling NVD for {}={}: {}", idParamName, id, e.getMessage());
        }
    }

    private void applyNvdData(EnrichedFinding enrichedFinding, String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode vulnerabilities = root.path("vulnerabilities");
            if (vulnerabilities.isArray() && vulnerabilities.size() > 0) {
                JsonNode cveNode = vulnerabilities.get(0).path("cve");
                
                String cveId = cveNode.path("id").asText(null);
                
                // Extract description
                String description = null;
                JsonNode descriptions = cveNode.path("descriptions");
                if (descriptions.isArray()) {
                    for (JsonNode desc : descriptions) {
                        if ("en".equals(desc.path("lang").asText())) {
                            description = desc.path("value").asText();
                            break;
                        }
                    }
                }
                
                // Extract CVSS score
                Double cvssScore = null;
                JsonNode metrics = cveNode.path("metrics");
                JsonNode cvssMetricV31 = metrics.path("cvssMetricV31");
                if (cvssMetricV31.isArray() && cvssMetricV31.size() > 0) {
                    JsonNode cvssData = cvssMetricV31.get(0).path("cvssData");
                    cvssScore = cvssData.path("baseScore").asDouble();
                }

                if (cvssScore != null) {
                    enrichedFinding.getFinding().setCvssScore(cvssScore);
                }
                if (description != null) {
                    enrichedFinding.getFinding().setNvdDescription(description);
                    
                    // Also update the main description if it's currently a placeholder or empty
                    if (isPlaceholder(enrichedFinding.getFinding().getDescription())) {
                        enrichedFinding.getFinding().setDescription(description);
                    }
                    if (enrichedFinding.getExplanation() != null && isPlaceholder(enrichedFinding.getExplanation().getDescription())) {
                        enrichedFinding.getExplanation().setDescription(description);
                    }
                }
                if (cveId != null && enrichedFinding.getFinding().getCveId() == null) {
                    enrichedFinding.getFinding().setCveId(cveId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse NVD JSON response: {}", e.getMessage());
        }
    }

    private boolean isPlaceholder(String description) {
        if (description == null || description.isBlank()) return true;
        String desc = description.toLowerCase();
        return desc.contains("no description available") || 
               desc.contains("potential security vulnerability detected") ||
               desc.contains("review code manually");
    }
}
