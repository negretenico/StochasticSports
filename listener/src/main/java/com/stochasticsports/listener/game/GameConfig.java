package com.stochasticsports.listener.game;

import com.stochasticsports.listener.config.ListenerProperties;
import com.stochasticsports.listener.event.EventProducer;
import com.stochasticsports.listener.event.NormalizedEvent;
import com.stochasticsports.listener.feed.FeedClient;
import com.stochasticsports.listener.feed.GamePollerFactory;
import com.stochasticsports.listener.feed.MlbFeedResponse;
import com.stochasticsports.listener.feed.Normalizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;

/**
 * Wires game-domain beans: normalizer, schedule discovery, poller factory, and the runner.
 */
@Configuration
public class GameConfig {

    @Bean
    public BiFunction<MlbFeedResponse, Integer, List<NormalizedEvent>> normalizer() {
        return new Normalizer();
    }

    @Bean
    public GameScheduleClient gameScheduleClient(WebClient mlbWebClient) {
        return new GameScheduleClient(mlbWebClient);
    }

    @Bean
    public GamePollerFactory gamePollerFactory(
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService gamePollerScheduler,
            BiFunction<MlbFeedResponse, Integer, List<NormalizedEvent>> normalizer) {
        return new GamePollerFactory(feedClient, producer, gamePollerScheduler, normalizer);
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
