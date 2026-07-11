package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;
import com.stochasticsports.listener.event.NormalizedEvent;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Drives the poll loop for a single MLB game.
 * Holds a ScheduledFuture and reschedules when the state machine transitions
 * to a state with a different poll interval. Stops permanently on FinalState.
 */
@Slf4j
public class GamePoller {

    private final int gamePk;
    private final FeedClient feedClient;
    private final EventProducer producer;
    private final ScheduledExecutorService scheduler;
    private final BiFunction<MlbFeedResponse, Integer, List<NormalizedEvent>> normalizer;

    private MlbGameState currentState;
    private ScheduledFuture<?> future;

    @Builder
    public GamePoller(
            MlbGameState initialState,
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService scheduler,
            BiFunction<MlbFeedResponse, Integer, List<NormalizedEvent>> normalizer) {
        this.gamePk       = initialState.gamePk();
        this.currentState = initialState;
        this.feedClient   = feedClient;
        this.producer     = producer;
        this.scheduler    = scheduler;
        this.normalizer   = normalizer;
    }

    public void start() {
        long delaySecs = currentState.pollInterval().toSeconds();
        future = scheduler.scheduleWithFixedDelay(this::poll, delaySecs, delaySecs, TimeUnit.SECONDS);
        log.info("Started poller for gamePk={} in state={} interval={}s",
                gamePk, currentState.getClass().getSimpleName(), delaySecs);
    }

    public void poll() {
        var feed = fetchFeed();
        if (Objects.isNull(feed)) return;
        advanceState(feed);
    }

    private MlbFeedResponse fetchFeed() {
        try {
            return feedClient.fetch(gamePk, currentState.lastTimecode().orElse(null));
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.warn("No feed for gamePk={} ({}): game may not have started", gamePk, e.getStatusCode().value());
                return null;
            }
            log.error("HTTP error polling gamePk={}: {}", gamePk, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("HTTP error polling gamePk={}: {}", gamePk, e.getMessage());
            return null;
        }
    }

    private void advanceState(MlbFeedResponse feed) {
        int newLastAtBatIndex = emitEvents(feed);
        var nextState = computeNextState(feed, newLastAtBatIndex);
        rescheduleIfNeeded(nextState, feed);
        currentState = nextState;
    }

    private int emitEvents(MlbFeedResponse feed) {
        try {
            return switch (currentState) {
                case LiveState ls -> ls.emit(feed, producer, normalizer);
                default           -> -1;
            };
        } catch (RuntimeException e) {
            log.error("Emit error for gamePk={}: {}", gamePk, e.getMessage(), e);
            return -1;
        }
    }

    private MlbGameState computeNextState(MlbFeedResponse feed, int newLastAtBatIndex) {
        var nextState = currentState.transition(feed);
        return nextState instanceof LiveState ls ? ls.withLastAtBatIndex(newLastAtBatIndex) : nextState;
    }

    private void rescheduleIfNeeded(MlbGameState nextState, MlbFeedResponse feed) {
        if (nextState.getClass().equals(currentState.getClass())) return;

        future.cancel(false);

        if (nextState.isTerminal()) {
            log.info("Game gamePk={} reached Final state — poller stopped", gamePk);
            return;
        }

        long delaySecs = switch (nextState) {
            case LiveState live -> live.pollIntervalFromFeed(feed).toSeconds();
            default             -> nextState.pollInterval().toSeconds();
        };
        future = scheduler.scheduleWithFixedDelay(this::poll, delaySecs, delaySecs, TimeUnit.SECONDS);
        log.info("Transitioned gamePk={} to {} interval={}s",
                gamePk, nextState.getClass().getSimpleName(), delaySecs);
    }
}
