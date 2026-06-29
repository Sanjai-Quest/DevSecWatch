package com.devsecwatch.worker.service;

import com.devsecwatch.worker.exception.ScanExecutionException;
import com.devsecwatch.worker.exception.OutOfMemoryException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SemgrepSandboxTest {

    private ObjectMapper objectMapper;
    private ScanProcessRegistry processRegistry;
    private TestSemgrepService semgrepService;

    @BeforeEach
    public void setup() {
        objectMapper = new ObjectMapper();
        processRegistry = mock(ScanProcessRegistry.class);
        semgrepService = new TestSemgrepService(objectMapper, processRegistry);
    }

    @Test
    public void testSemgrepDockerCommandFlags(@TempDir Path tempDir) {
        // Setup mock process
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("{\"results\":[]}".getBytes()));
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        
        try {
            when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
            when(mockProcess.exitValue()).thenReturn(0);
        } catch (InterruptedException e) {
            fail(e);
        }

        ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
        try {
            when(mockBuilder.start()).thenReturn(mockProcess);
        } catch (IOException e) {
            fail(e);
        }

        semgrepService.mockProcessBuilder = mockBuilder;

        // Run
        semgrepService.runScan(tempDir, 123L);

        // Verify command flags
        List<String> capturedCmd = semgrepService.capturedCmd;
        assertNotNull(capturedCmd, "Command should not be null");
        assertTrue(capturedCmd.contains("docker"), "Command should use docker");
        assertTrue(capturedCmd.contains("--network=none"), "Command must disable network");
        assertTrue(capturedCmd.contains("--read-only"), "Command must be read-only");
        assertTrue(capturedCmd.contains("--memory=512m"), "Command must have memory limit");
        assertTrue(capturedCmd.contains("--cpus=0.5"), "Command must have cpu limit");
        assertTrue(capturedCmd.contains("--pids-limit=50"), "Command must have pids limit");
        assertTrue(capturedCmd.contains("--cap-drop=ALL"), "Command must drop capabilities");
        assertTrue(capturedCmd.contains("--security-opt=no-new-privileges"), "Command must have no-new-privileges");
        assertTrue(capturedCmd.contains("--user=1000:1000"), "Command must specify non-root user");
        
        // Verify mount path
        boolean hasVolumeMount = capturedCmd.stream().anyMatch(arg -> arg.startsWith("-v") || arg.contains(":/src:ro"));
        assertTrue(hasVolumeMount, "Command must mount the repo as read-only");
    }

    @Test
    public void testSemgrepDockerNonZeroExitCode(@TempDir Path tempDir) {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        
        try {
            when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
            // 125 is a typical docker run failure code
            when(mockProcess.exitValue()).thenReturn(125);
        } catch (InterruptedException e) {
            fail(e);
        }

        ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
        try {
            when(mockBuilder.start()).thenReturn(mockProcess);
        } catch (IOException e) {
            fail(e);
        }

        semgrepService.mockProcessBuilder = mockBuilder;

        ScanExecutionException exception = assertThrows(ScanExecutionException.class, () -> {
            semgrepService.runScan(tempDir, 123L);
        });

        assertTrue(exception.getMessage().contains("Docker Semgrep failed with exit code: 125"));
    }

    @Test
    public void testSemgrepDockerOomExitCode(@TempDir Path tempDir) {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        
        try {
            when(mockProcess.waitFor(anyLong(), any())).thenReturn(true);
            when(mockProcess.exitValue()).thenReturn(137);
        } catch (InterruptedException e) {
            fail(e);
        }

        ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
        try {
            when(mockBuilder.start()).thenReturn(mockProcess);
        } catch (IOException e) {
            fail(e);
        }

        semgrepService.mockProcessBuilder = mockBuilder;

        OutOfMemoryException exception = assertThrows(OutOfMemoryException.class, () -> {
            semgrepService.runScan(tempDir, 123L);
        });

        assertTrue(exception.getMessage().contains("breached 512MB limit"));
    }

    @Test
    public void testSemgrepDockerTimeout(@TempDir Path tempDir) {
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        
        try {
            when(mockProcess.waitFor(anyLong(), any())).thenReturn(false); // Simulate timeout
        } catch (InterruptedException e) {
            fail(e);
        }

        ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
        try {
            when(mockBuilder.start()).thenReturn(mockProcess);
        } catch (IOException e) {
            fail(e);
        }

        semgrepService.mockProcessBuilder = mockBuilder;

        ScanExecutionException exception = assertThrows(ScanExecutionException.class, () -> {
            semgrepService.runScan(tempDir, 123L);
        });

        assertTrue(exception.getMessage().contains("Semgrep timeout"));
        verify(mockProcess, times(1)).destroyForcibly();
    }

    // Helper subclass to intercept ProcessBuilder
    private static class TestSemgrepService extends SemgrepService {
        List<String> capturedCmd;
        ProcessBuilder mockProcessBuilder;

        public TestSemgrepService(ObjectMapper objectMapper, ScanProcessRegistry processRegistry) {
            super(objectMapper, processRegistry);
        }

        @Override
        protected ProcessBuilder createProcessBuilder(List<String> cmd) {
            this.capturedCmd = cmd;
            return mockProcessBuilder != null ? mockProcessBuilder : new ProcessBuilder(cmd);
        }
    }
}
