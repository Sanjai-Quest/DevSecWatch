package com.devsecwatch.worker.service;

import com.devsecwatch.worker.exception.GitCloneException;
import com.devsecwatch.worker.exception.GitCloneTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);
    private final ScanProcessRegistry processRegistry;

    @Value("${app.temp-directory:C:/tmp/devsecwatch}")
    private String tempDirectory;

    public GitService(ScanProcessRegistry processRegistry) {
        this.processRegistry = processRegistry;
    }

    public Path cloneRepository(String repoUrl, String branch, Long scanId) {
        String dirName = tempDirectory + File.separator + "scan-" + scanId + "-" + UUID.randomUUID();
        File directory = new File(dirName);
        directory.getParentFile().mkdirs();

        log.info("Cloning repository {} (branch: {}) to {}", repoUrl, branch, directory.getAbsolutePath());

        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.add("clone");
        cmd.add("--depth=1");
        cmd.add("--single-branch");
        cmd.add("-b");
        cmd.add(branch);
        cmd.add(repoUrl);
        cmd.add(directory.getAbsolutePath());

        long startTime = System.currentTimeMillis();
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("GIT_TERMINAL_PROMPT", "0");
            pb.redirectErrorStream(true);

            process = pb.start();
            processRegistry.registerProcess(scanId, process);

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                throw new GitCloneTimeoutException("Git clone timed out after 5 minutes for: " + repoUrl);
            }

            if (process.exitValue() != 0) {
                log.error("git clone failed (exit {}): {}", process.exitValue(), output);
                if (output.contains("Remote branch") && output.contains("not found")) {
                    log.info("Branch '{}' not found, retrying with default branch...", branch);
                    return cloneDefaultBranch(repoUrl, scanId, directory);
                }
                throw new GitCloneException("Failed to clone repository: " + repoUrl + " — " + output.trim());
            }

            log.info("Repository cloned successfully in {} ms. Output: {}", duration, output.trim());
            return Paths.get(directory.getAbsolutePath());

        } catch (GitCloneException | GitCloneTimeoutException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCloneException("Git clone was interrupted: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to start git clone process: {}", e.getMessage());
            throw new GitCloneException("Failed to start git clone: " + e.getMessage());
        } finally {
            if (process != null) {
                processRegistry.unregisterProcess(scanId, process);
            }
        }
    }

    private Path cloneDefaultBranch(String repoUrl, Long scanId, File directory) {
        if (directory.exists()) {
            try (Stream<Path> walk = Files.walk(directory.toPath())) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException ignored) {}
        }

        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.add("clone");
        cmd.add("--depth=1");
        cmd.add(repoUrl);
        cmd.add(directory.getAbsolutePath());

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("GIT_TERMINAL_PROMPT", "0");
            pb.redirectErrorStream(true);

            process = pb.start();
            processRegistry.registerProcess(scanId, process);

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new GitCloneTimeoutException("Git clone (default branch) timed out after 5 minutes for: " + repoUrl);
            }

            if (process.exitValue() != 0) {
                throw new GitCloneException("Failed to clone repository (default branch): " + repoUrl + " — " + output.trim());
            }

            log.info("Repository cloned with default branch. Output: {}", output.trim());
            return Paths.get(directory.getAbsolutePath());

        } catch (GitCloneException | GitCloneTimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new GitCloneException("Unexpected error during default-branch clone: " + e.getMessage());
        } finally {
            if (process != null) {
                processRegistry.unregisterProcess(scanId, process);
            }
        }
    }

    public void cleanup(Path repoPath) {
        if (repoPath == null || !Files.exists(repoPath)) return;
        try (Stream<Path> walk = Files.walk(repoPath)) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            log.info("Cleaned up temporary directory: {}", repoPath);
        } catch (IOException e) {
            log.warn("Failed to clean up temporary directory {}: {}", repoPath, e.getMessage());
        }
    }
}
