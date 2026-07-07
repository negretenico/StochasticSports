package com.stochasticsports.listener.event;

/**
 * Port for sending normalized events to the downstream Kafka topic.
 * Domain interface — no Spring or Kafka imports here.
 */
public interface EventProducer {
    void send(NormalizedEvent event);
}
