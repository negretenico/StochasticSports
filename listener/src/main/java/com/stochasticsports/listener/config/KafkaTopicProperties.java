package com.stochasticsports.listener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Topic configuration — decoupled from the listener service.
 * Bound from the "kafka.topic" prefix in application.yaml.
 */
@ConfigurationProperties(prefix = "kafka.topic")
public record KafkaTopicProperties(
        String name,
        int    partitions,
        int    replicas
) {}
