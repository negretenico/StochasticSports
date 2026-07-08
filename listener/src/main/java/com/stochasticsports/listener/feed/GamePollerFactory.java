package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Creates GamePoller instances. Encapsulates the three shared dependencies
 * (FeedClient, EventProducer, ScheduledExecutorService) so GameDiscoveryService
 * only needs one collaborator to start new pollers.
 */
public class GamePollerFactory {

    private final FeedClient feedClient;
    private final EventProducer producer;
    private final ScheduledExecutorService scheduler;

    public GamePollerFactory(FeedClient feedClient, EventProducer producer, ScheduledExecutorService scheduler) {
        this.feedClient = feedClient;
        this.producer   = producer;
        this.scheduler  = scheduler;
    }

    public GamePoller create(int gamePk) {
        return new GamePoller(gamePk, new PreviewState(gamePk), feedClient, producer, scheduler);
    }
}
