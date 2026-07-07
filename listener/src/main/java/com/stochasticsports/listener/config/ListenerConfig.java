package com.stochasticsports.listener.config;

import com.stochasticsports.listener.feed.FeedClient;
import com.stochasticsports.listener.game.GameDiscoveryService;
import com.stochasticsports.listener.game.GameScheduleClient;
import com.stochasticsports.listener.event.EventProducer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Main wiring configuration for the listener module.
 * One @Configuration class per module — Spring knowledge stays here.
 * Domain classes (FeedClient, Normalizer, etc.) have zero Spring imports.
 */
@Configuration
@EnableConfigurationProperties(ListenerProperties.class)
public class ListenerConfig {

    private final ListenerProperties props;

    public ListenerConfig(ListenerProperties props) {
        this.props = props;
    }

    @Bean
    public WebClient mlbWebClient() {
        return WebClient.builder()
                .baseUrl(props.mlbApiBaseUrl())
                .build();
    }

    @Bean
    public FeedClient feedClient(WebClient mlbWebClient) {
        return new FeedClient(mlbWebClient);
    }

    @Bean
    public GameScheduleClient gameScheduleClient(WebClient mlbWebClient) {
        return new GameScheduleClient(mlbWebClient);
    }

    @Bean
    public ScheduledExecutorService gamePollerScheduler() {
        // virtual threads (Java 21) for lightweight per-game scheduling
        return Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors() * 2);
    }

    @Bean
    public GameDiscoveryService gameDiscoveryService(
            GameScheduleClient scheduleClient,
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService gamePollerScheduler) {
        return new GameDiscoveryService(scheduleClient, feedClient, producer, gamePollerScheduler, props);
    }
}
