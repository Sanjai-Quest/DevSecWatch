package com.devsecwatch.worker;

import com.devsecwatch.worker.service.DependencyScanner;
import com.devsecwatch.worker.model.EnrichedFinding;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DependencyScannerTest {

    @Test
    public void testScanDependencies() {
        ObjectMapper mapper = new ObjectMapper();
        DependencyScanner scanner = new DependencyScanner(mapper);

        Path repoPath = Paths.get("s:/Project Folders/DevSecWatch/devsecwatch-frontend");
        List<EnrichedFinding> findings = scanner.scanDependencies(repoPath);

        assertNotNull(findings);
        System.out.println("Found " + findings.size() + " vulnerable dependencies in frontend");
        
        for (EnrichedFinding ef : findings) {
            System.out.println("Vulnerability: " + ef.getFinding().getRuleId());
            System.out.println("Package: " + ef.getFinding().getCodeSnippet());
            System.out.println("Severity: " + ef.getFinding().getSeverity());
            System.out.println("Fix: " + ef.getExplanation().getFixSuggestion());
        }
    }
}
