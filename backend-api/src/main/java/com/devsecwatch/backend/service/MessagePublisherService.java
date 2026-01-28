package com.devsecwatch.backend.service;

import com.devsecwatch.backend.dto.message.ScanMessage;
import com.devsecwatch.backend.exception.MessagePublishException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessagePublisherService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.scans:scans-exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.scan-new:scan.new}")
    private String routingKey;

    public void publishScanJob(ScanMessage message) {
        if (message.getCorrelationId() == null) {
            message.setCorrelationId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        log.info("Publishing scan job with correlation ID: {}", message.getCorrelationId());

        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message, m -> {
                m.getMessageProperties().setHeader("correlationId", message.getCorrelationId());
                return m;
            });
            log.info("Successfully published scan job for scan ID: {}", message.getScanId());
        } catch (AmqpException e) {
            log.error("Failed to publish scan job: {}", e.getMessage(), e);
            throw new MessagePublishException("Failed to publish scan job", e);
        }
    }
}
