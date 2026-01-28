package com.devsecwatch.backend.config;

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
    public static final String FAILED_SCANS_QUEUE = "failed-scans";
    public static final String SCAN_FAILED_ROUTING_KEY = "scan.failed";

    @Bean
    public DirectExchange scansExchange() {
        return new DirectExchange(SCANS_EXCHANGE, true, false);
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
    public Binding scanBinding() {
        return BindingBuilder.bind(scanJobsQueue()).to(scansExchange()).with(SCAN_NEW_ROUTING_KEY);
    }

    @Bean
    public DirectExchange failedScansExchange() {
        return new DirectExchange(FAILED_SCANS_EXCHANGE, true, false);
    }

    @Bean
    public Queue failedScansQueue() {
        return new Queue(FAILED_SCANS_QUEUE, true);
    }

    @Bean
    public Binding failedBinding() {
        return BindingBuilder.bind(failedScansQueue()).to(failedScansExchange()).with(SCAN_FAILED_ROUTING_KEY);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue("scan.notifications", true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
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
        return factory;
    }
}
