package com.devsecwatch.worker.service;

import com.devsecwatch.worker.model.EnrichedFinding;
import com.devsecwatch.worker.model.Finding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class NvdEnrichmentService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public List<EnrichedFinding> enrichWithNvdInfo(List<EnrichedFinding> findings) {
        for (EnrichedFinding enrichedFinding : findings) {
            Finding finding = enrichedFinding.getFinding();
            String cweId = finding.getCweId();
            
            if (cweId != null && !cweId.isBlank()) {
                // Ensure CWE is formatted correctly, e.g. CWE-79
                String parsedCwe = extractCweFormat(cweId);
                if (parsedCwe != null) {
                    try {
                        enrichSingleFinding(enrichedFinding, parsedCwe);
                    } catch (Exception e) {
                        log.warn("Failed to enrich finding with NVD for CWE {}: {}", parsedCwe, e.getMessage());
                    }
                }
            }
        }
        return findings;
    }

    private String extractCweFormat(String rawCwe) {
        Pattern pattern = Pattern.compile("CWE-\\d+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(rawCwe);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null; // Don't mistakenly search for a non-CWE string
    }

    private void enrichSingleFinding(EnrichedFinding enrichedFinding, String cweId) {
        String redisKey = "nvd:" + cweId;
        String cachedResult = redisTemplate.opsForValue().get(redisKey);

        if (cachedResult != null) {
            log.debug("NVD Cache hit for {}", cweId);
            applyNvdData(enrichedFinding, cachedResult);
            return;
        }

        log.info("Fetching NVD data for {}", cweId);
        String url = "https://services.nvd.nist.gov/rest/json/cves/2.0?cweId=" + cweId + "&resultsPerPage=1";
        
        try {
            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                // Cache for 24 hours to avoid rate limiting
                redisTemplate.opsForValue().set(redisKey, response, 24, TimeUnit.HOURS);
                applyNvdData(enrichedFinding, response);
            }
        } catch (Exception e) {
            log.error("API error when calling NVD for {}: {}", cweId, e.getMessage());
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
                }
                if (cveId != null && enrichedFinding.getFinding().getCveId() == null) {
                    enrichedFinding.getFinding().setCveId(cveId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse NVD JSON response: {}", e.getMessage());
        }
    }
}
