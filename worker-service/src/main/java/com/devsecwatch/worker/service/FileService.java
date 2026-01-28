package com.devsecwatch.worker.service;

import com.devsecwatch.worker.exception.NoFilesFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class FileService {

    public List<Path> extractJavaFiles(Path repoRoot) {
        try (Stream<Path> walk = Files.walk(repoRoot, Integer.MAX_VALUE)) {
            List<Path> codeFiles = walk.filter(Files::isRegularFile)
                    // Include all common source code file extensions
                    .filter(p -> {
                        String path = p.toString().toLowerCase();
                        return path.endsWith(".java") || path.endsWith(".js") || path.endsWith(".jsx")
                                || path.endsWith(".ts") || path.endsWith(".tsx") || path.endsWith(".py")
                                || path.endsWith(".go") || path.endsWith(".rb") || path.endsWith(".php")
                                || path.endsWith(".c") || path.endsWith(".cpp") || path.endsWith(".cs")
                                || path.endsWith(".swift") || path.endsWith(".kt") || path.endsWith(".rs");
                    })
                    .filter(p -> !p.toString().contains("/target/"))
                    .filter(p -> !p.toString().contains("\\target\\"))
                    .filter(p -> !p.toString().contains("/build/"))
                    .filter(p -> !p.toString().contains("\\build\\"))
                    .filter(p -> !p.toString().contains("/node_modules/"))
                    .filter(p -> !p.toString().contains("\\node_modules\\"))
                    .filter(p -> !p.toString().contains("/dist/"))
                    .filter(p -> !p.toString().contains("\\dist\\"))
                    .filter(p -> !p.toString().contains("/.git/"))
                    .filter(p -> !p.toString().contains("\\.git\\"))
                    .collect(Collectors.toList());

            if (codeFiles.isEmpty()) {
                throw new NoFilesFoundException("No source code files found in repository");
            }

            log.info("Found {} source code files", codeFiles.size());
            return codeFiles;
        } catch (IOException e) {
            log.error("Error extracting source files: {}", e.getMessage());
            throw new RuntimeException("Error extracting files", e);
        }
    }

    public int countLinesOfCode(List<Path> files) {
        int count = 0;
        for (Path file : files) {
            try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
                count += lines.filter(line -> !line.trim().isEmpty())
                        .filter(line -> !line.trim().startsWith("//"))
                        .filter(line -> !line.trim().startsWith("/*"))
                        .filter(line -> !line.trim().startsWith("*"))
                        .count();
            } catch (IOException e) {
                log.warn("Failed to read file {}: {}", file, e.getMessage());
            }
        }
        log.info("Total lines of code: {}", count);
        return count;
    }
}
