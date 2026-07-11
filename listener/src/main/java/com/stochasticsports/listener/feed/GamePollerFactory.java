package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;
import com.stochasticsports.listener.event.NormalizedEvent;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;

/**
 * Creates GamePoller instances. Encapsulates shared dependencies so
 * GameDiscoveryService only needs one collaborator to start new pollers.
 */
public class GamePollerFactory {

    private final FeedClient feedClient;
    private final EventProducer producer;
    private final ScheduledExecutorService scheduler;
    private final BiFunction<MlbFeedResponse, Integer, List<NormalizedEvent>> normalizer;

    public GamePollerFactory(
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService scheduler,
            BiFunction<MlbFeedResponse, Integer, List<NormalizedEvent>> normalizer) {
        this.feedClient = feedClient;
        this.producer   = producer;
        this.scheduler  = scheduler;
        this.normalizer = normalizer;
    }

    public GamePoller create(int gamePk) {
        return GamePoller.builder()
                .initialState(new PreviewState(gamePk))
                .feedClient(feedClient)
                .producer(producer)
                .scheduler(scheduler)
                .normalizer(normalizer)
                .build();
    }
}
