package com.devsecwatch.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GitService {

    public boolean isRepositoryAccessible(String repoUrl) {
        // Simple check using git ls-remote to verify repo exists and is public
        // Just checking HEAD is sufficient to validate accessibility
        ProcessBuilder processBuilder = new ProcessBuilder("git", "ls-remote", repoUrl, "HEAD");

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);

            if (!finished) {
                process.destroy();
                log.warn("Timed out checking repository accessibility: {}", repoUrl);
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("Repository access check failed for {}. Exit code: {}", repoUrl, exitCode);
                return false;
            }

            return true;
        } catch (IOException | InterruptedException e) {
            log.error("Error checking repository accessibility: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
