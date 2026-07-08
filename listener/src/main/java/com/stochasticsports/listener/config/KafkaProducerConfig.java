package com.stochasticsports.listener.config;

import com.stochasticsports.listener.event.KafkaEventProducer;
import com.stochasticsports.listener.event.NormalizedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Wires the Kafka producer adapter and declares the mlb topic.
 * KafkaTemplate auto-configured by spring-kafka based on application.yaml.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaEventProducer kafkaEventProducer(KafkaTemplate<String, NormalizedEvent> kafkaTemplate) {
        return new KafkaEventProducer(kafkaTemplate);
    }

    /** Declares the mlb topic idempotently on startup (no-op if it already exists). */
    @Bean
    public NewTopic mlbTopic() {
        return TopicBuilder.name("mlb")
                .partitions(16)
                .replicas(1)
                .build();
    }
}
