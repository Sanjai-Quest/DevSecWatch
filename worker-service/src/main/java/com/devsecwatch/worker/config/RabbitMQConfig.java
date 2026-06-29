package com.devsecwatch.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String SCANS_EXCHANGE = "scans-exchange";
    public static final String SCAN_JOBS_QUEUE = "scan-jobs";
    public static final String SCAN_NEW_ROUTING_KEY = "scan.new";
    public static final String FAILED_SCANS_EXCHANGE = "failed-scans-exchange";
    public static final String AI_ENRICHMENT_EXCHANGE = "ai-enrichment-exchange";
    public static final String AI_ENRICHMENT_QUEUE = "ai-enrichment-jobs";
    public static final String AI_ENRICHMENT_ROUTING_KEY = "ai.enrich";
    public static final String SCAN_CANCELLATION_EXCHANGE = "scan-cancellation-exchange";

    @Bean
    public FanoutExchange cancellationExchange() {
        return new FanoutExchange(SCAN_CANCELLATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue cancellationQueue() {
        // Unique queue for each worker instance to receive fanout broadcast
        return new AnonymousQueue();
    }

    @Bean
    public Binding cancellationBinding(Queue cancellationQueue, FanoutExchange cancellationExchange) {
        return BindingBuilder.bind(cancellationQueue).to(cancellationExchange);
    }

    @Bean
    public DirectExchange scansExchange() {
        return new DirectExchange(SCANS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(AI_ENRICHMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue scanJobsQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 600000); // 10 minutes
        args.put("x-max-length", 100);
        args.put("x-dead-letter-exchange", FAILED_SCANS_EXCHANGE);
        return new Queue(SCAN_JOBS_QUEUE, true, false, false, args);
    }

    @Bean
    public FanoutExchange failedScansExchange() {
        return new FanoutExchange(FAILED_SCANS_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue("scan-jobs-dlq", true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(failedScansExchange());
    }

    @Bean
    public Queue aiEnrichmentQueue() {
        return new Queue(AI_ENRICHMENT_QUEUE, true);
    }

    @Bean
    public Binding scanBinding() {
        return BindingBuilder.bind(scanJobsQueue()).to(scansExchange()).with(SCAN_NEW_ROUTING_KEY);
    }

    @Bean
    public Binding aiBinding() {
        return BindingBuilder.bind(aiEnrichmentQueue()).to(aiExchange()).with(AI_ENRICHMENT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory = new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
