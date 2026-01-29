package com.devsecwatch.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GitService {

    public boolean isRepositoryAccessible(String repoUrl) {
        // TODO: Implement using GitHub API instead of git command
        // For now, skip the check - validation will happen during actual clone in
        // worker
        log.info("Skipping pre-check for repository: {}", repoUrl);
        return true;
    }
}
