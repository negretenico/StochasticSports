package com.stochasticsports.listener.config;

import com.stochasticsports.listener.event.KafkaEventProducer;
import com.stochasticsports.listener.event.NormalizedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Wires the Kafka producer adapter and declares the topic.
 * KafkaTemplate auto-configured by spring-kafka based on application.yaml.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaProducerConfig {

    @Bean
    public KafkaEventProducer kafkaEventProducer(
            KafkaTemplate<String, NormalizedEvent> kafkaTemplate,
            KafkaTopicProperties props) {
        return new KafkaEventProducer(kafkaTemplate, props.name());
    }

    /** Declares the topic idempotently on startup (no-op if it already exists). */
    @Bean
    public NewTopic mlbTopic(KafkaTopicProperties props) {
        return TopicBuilder.name(props.name())
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }
}
