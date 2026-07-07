package com.stochasticsports.listener.config;

import com.stochasticsports.listener.event.KafkaEventProducer;
import com.stochasticsports.listener.event.NormalizedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Wires the Kafka producer adapter.
 * KafkaTemplate auto-configured by spring-kafka based on application.yaml.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaEventProducer kafkaEventProducer(KafkaTemplate<String, NormalizedEvent> kafkaTemplate) {
        return new KafkaEventProducer(kafkaTemplate);
    }
}
