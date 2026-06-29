package com.devsecwatch.worker.worker;

import com.devsecwatch.worker.dto.message.ScanCancellationMessage;
import com.devsecwatch.worker.service.ScanProcessRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ScanCancellationListener {

    private static final Logger log = LoggerFactory.getLogger(ScanCancellationListener.class);
    private final ScanProcessRegistry processRegistry;

    public ScanCancellationListener(ScanProcessRegistry processRegistry) {
        this.processRegistry = processRegistry;
    }

    @RabbitListener(queues = "#{cancellationQueue.name}")
    public void handleCancellation(ScanCancellationMessage message) {
        log.info("Received cancellation request for scan ID: {}", message.getScanId());
        processRegistry.cancel(message.getScanId());
    }
}
