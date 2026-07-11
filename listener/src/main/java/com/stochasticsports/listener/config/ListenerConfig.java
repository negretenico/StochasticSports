package com.stochasticsports.listener.config;

import com.stochasticsports.listener.feed.FeedClient;
import com.stochasticsports.listener.feed.GamePollerFactory;
import com.stochasticsports.listener.game.DiscoveryRunner;
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

    @Bean
    public WebClient mlbWebClient(ListenerProperties props) {
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
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }

    @Bean
    public GamePollerFactory gamePollerFactory(
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService gamePollerScheduler) {
        return new GamePollerFactory(feedClient, producer, gamePollerScheduler);
    }

    @Bean
    public GameDiscoveryService gameDiscoveryService(
            GameScheduleClient scheduleClient,
            GamePollerFactory gamePollerFactory,
            ListenerProperties props) {
        return new GameDiscoveryService(scheduleClient, gamePollerFactory, props);
    }

    @Bean
    public DiscoveryRunner discoveryRunner(
            GameDiscoveryService gameDiscoveryService,
            ScheduledExecutorService gamePollerScheduler,
            ListenerProperties props) {
        return new DiscoveryRunner(gameDiscoveryService, gamePollerScheduler, props.discoveryIntervalSeconds());
    }
}
