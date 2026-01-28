package com.devsecwatch.worker.service;

import com.devsecwatch.worker.exception.GitCloneException;
import com.devsecwatch.worker.exception.GitCloneTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitService {

    @Value("${app.temp-directory:/tmp/devsecwatch}")
    private String tempDirectory;

    public Path cloneRepository(String repoUrl, String branch, Long scanId) {
        String dirName = tempDirectory + File.separator + "scan-" + scanId + "-" + UUID.randomUUID();
        File directory = new File(dirName);

        log.info("Cloning repository {} (branch: {}) to {}", repoUrl, branch, directory.getAbsolutePath());

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(directory)
                .setBranchesToClone(Collections.singleton("refs/heads/" + branch))
                .setBranch("refs/heads/" + branch)
                .setCloneSubmodules(false)
                .setTimeout(60); // 60 seconds timeout

        long startTime = System.currentTimeMillis();
        try (Git git = cloneCommand.call()) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Repository cloned successfully in {} ms", duration);
            return Paths.get(directory.getAbsolutePath());
        } catch (GitAPIException e) {
            long duration = System.currentTimeMillis() - startTime;
            if (duration >= 60000) {
                throw new GitCloneTimeoutException("Git clone operation timed out");
            }
            log.error("Failed to clone repository: {}", e.getMessage());
            throw new GitCloneException("Failed to clone repository: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during git clone: {}", e.getMessage());
            throw new GitCloneException("Unexpected error during git clone: " + e.getMessage());
        }
    }

    public void cleanup(Path repoPath) {
        if (repoPath == null || !Files.exists(repoPath)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(repoPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            log.info("Cleaned up temporary directory: {}", repoPath);
        } catch (IOException e) {
            log.warn("Failed to clean up temporary directory {}: {}", repoPath, e.getMessage());
        }
    }
}
