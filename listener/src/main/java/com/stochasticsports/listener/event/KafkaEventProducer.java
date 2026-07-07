package com.stochasticsports.listener.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka adapter implementing the EventProducer port.
 * Topic: "mlb". Key: gamePk as String.
 */
@Slf4j
public class KafkaEventProducer implements EventProducer {

    static final String TOPIC = "mlb";

    private final KafkaTemplate<String, NormalizedEvent> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, NormalizedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(NormalizedEvent event) {
        var key = String.valueOf(event.gamePk());
        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event eventId={} gamePk={}: {}",
                                event.eventId(), event.gamePk(), ex.getMessage());
                    } else {
                        log.debug("Sent event eventId={} gamePk={} partition={}",
                                event.eventId(), event.gamePk(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
