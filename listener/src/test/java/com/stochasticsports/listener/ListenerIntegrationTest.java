package com.stochasticsports.listener;

import com.stochasticsports.listener.event.NormalizedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cycle 11 — Integration: Spring context loads and KafkaTemplate bean is present.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 16,
        topics = {"mlb"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
class ListenerIntegrationTest {

    @Autowired(required = false)
    private KafkaTemplate<String, NormalizedEvent> kafkaTemplate;

    @Test
    void contextLoads_kafkaTemplateBeanIsPresent() {
        assertThat(kafkaTemplate).isNotNull();
    }
}
