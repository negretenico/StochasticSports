package com.stochasticsports.listener.game;

import com.stochasticsports.listener.config.ListenerProperties;
import com.stochasticsports.listener.event.EventProducer;
import com.stochasticsports.listener.feed.FeedClient;
import com.stochasticsports.listener.feed.GamePoller;
import com.stochasticsports.listener.feed.PreviewState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Periodically discovers active games and starts a GamePoller for each new gamePk.
 * Runs on a Spring @Scheduled task every discoveryIntervalSeconds (default 5 min).
 * Tracks already-started pollers so the same game is never double-scheduled.
 */
@Slf4j
public class GameDiscoveryService {

    private final GameScheduleClient scheduleClient;
    private final FeedClient feedClient;
    private final EventProducer producer;
    private final ScheduledExecutorService scheduler;
    private final ListenerProperties props;

    /** gamePks for which a GamePoller has already been started this process lifetime */
    private final Set<Integer> managedGames = ConcurrentHashMap.newKeySet();

    public GameDiscoveryService(
            GameScheduleClient scheduleClient,
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService scheduler,
            ListenerProperties props) {
        this.scheduleClient = scheduleClient;
        this.feedClient     = feedClient;
        this.producer       = producer;
        this.scheduler      = scheduler;
        this.props          = props;
    }

    @Scheduled(fixedDelayString = "${listener.discovery-interval-seconds}000")
    public void discoverAndRegister() {
        String date = resolveDate();
        log.info("Discovering active games for date={}", date);

        scheduleClient.fetchActiveGamePks(date).stream()
                .filter(pk -> !managedGames.contains(pk))
                .forEach(pk -> {
                    managedGames.add(pk);
                    var poller = new GamePoller(pk, new PreviewState(pk), feedClient, producer, scheduler);
                    poller.start();
                    log.info("Started GamePoller for gamePk={}", pk);
                });
    }

    private String resolveDate() {
        var configured = props.pollDate();
        return (configured == null || configured.isBlank())
                ? LocalDate.now().toString()
                : configured;
    }
}
