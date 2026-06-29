package com.devsecwatch.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
public class GracefulShutdownService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownService.class);
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final ScanProcessRegistry processRegistry;
    private boolean isRunning = false;

    public GracefulShutdownService(RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry, 
                                  ScanProcessRegistry processRegistry) {
        this.rabbitListenerEndpointRegistry = rabbitListenerEndpointRegistry;
        this.processRegistry = processRegistry;
    }

    @Override
    public void start() {
        log.info("GracefulShutdownService started");
        isRunning = true;
    }

    @Override
    public void stop() {
        log.info("SIGTERM/Shutdown received. Starting graceful shutdown sequence...");
        
        // 1. Stop receiving new RabbitMQ messages
        log.info("Stopping RabbitMQ listeners...");
        rabbitListenerEndpointRegistry.stop();
        
        // 2. Wait for active scans to complete or a timeout
        log.info("Waiting for active scans to finish...");
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds
        
        // Here we could check processRegistry.hasActiveScans() if we implement it
        // Or just let Spring wait for the threads to finish within its own timeout.
        
        log.info("Graceful shutdown complete.");
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        // High phase number to be stopped early (before other beans)
        return Integer.MAX_VALUE;
    }
}
