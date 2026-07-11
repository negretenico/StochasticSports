package com.stochasticsports.listener.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Objects;

/**
 * Kafka adapter implementing the EventProducer port.
 * Topic name and key (gamePk as String) are supplied at construction time.
 */
@Slf4j
public class KafkaEventProducer implements EventProducer {

    private final KafkaTemplate<String, NormalizedEvent> kafkaTemplate;
    private final String topic;

    public KafkaEventProducer(KafkaTemplate<String, NormalizedEvent> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void send(NormalizedEvent event) {
        var key = String.valueOf(event.gamePk());
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (Objects.isNull(ex)) {
                        log.debug("Sent event eventId={} partition={}",
                                event.eventId(), result.getRecordMetadata().partition());
                        return;
                    }
                    log.error("Failed to send event eventId={} gamePk={}: {}",
                            event.eventId(), event.gamePk(), ex.getMessage());
                });
    }
}
