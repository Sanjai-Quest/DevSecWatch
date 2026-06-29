package com.devsecwatch.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ScanProcessRegistry {

    private static final Logger log = LoggerFactory.getLogger(ScanProcessRegistry.class);

    // Track active threads by scanId
    private final Map<Long, Thread> activeThreads = new ConcurrentHashMap<>();
    
    // Track active native processes by scanId
    private final Map<Long, Set<Process>> activeProcesses = new ConcurrentHashMap<>();

    public void registerThread(Long scanId, Thread thread) {
        activeThreads.put(scanId, thread);
    }

    public void unregisterThread(Long scanId) {
        activeThreads.remove(scanId);
    }

    public void registerProcess(Long scanId, Process process) {
        activeProcesses.computeIfAbsent(scanId, k -> ConcurrentHashMap.newKeySet()).add(process);
    }

    public void unregisterProcess(Long scanId, Process process) {
        Set<Process> processes = activeProcesses.get(scanId);
        if (processes != null) {
            processes.remove(process);
            if (processes.isEmpty()) {
                activeProcesses.remove(scanId);
            }
        }
    }

    public void cancel(Long scanId) {
        log.info("Attempting to cancel scan ID: {}", scanId);
        
        // 1. Terminate native processes
        Set<Process> processes = activeProcesses.remove(scanId);
        if (processes != null) {
            log.info("Killing {} active processes for scan ID: {}", processes.size(), scanId);
            processes.forEach(p -> {
                if (p.isAlive()) {
                    p.destroyForcibly();
                }
            });
        }

        // 2. Interrupt the thread
        Thread thread = activeThreads.remove(scanId);
        if (thread != null) {
            log.info("Interrupting thread {} for scan ID: {}", thread.getName(), scanId);
            thread.interrupt();
        }
    }
    
    public void unregister(Long scanId) {
        activeThreads.remove(scanId);
        activeProcesses.remove(scanId);
    }

    public boolean isCancelled(Long scanId) {
        // Simple way to check if it's no longer in the registry while it should be
        // But better is to check if the thread is interrupted
        Thread t = activeThreads.get(scanId);
        return t != null && t.isInterrupted();
    }
}
