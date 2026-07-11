package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private MlbGameState currentState;
    private ScheduledFuture<?> future;

    @Builder
    public GamePoller(
            MlbGameState initialState,
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService scheduler) {
        this.gamePk       = initialState.gamePk();
        this.currentState = initialState;
        this.feedClient   = feedClient;
        this.producer     = producer;
        this.scheduler    = scheduler;
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
            } else {
                log.error("HTTP error polling gamePk={}: {}", gamePk, e.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.error("HTTP error polling gamePk={}: {}", gamePk, e.getMessage());
            return null;
        }
    }

    private void advanceState(MlbFeedResponse feed) {
        try {
            int newLastAtBatIndex = switch (currentState) {
                case LiveState ls  -> ls.emit(feed, producer);
                case PreviewState p -> -1;
                case FinalState f   -> -1;
            };

            var nextState = currentState.transition(feed);
            if (nextState instanceof LiveState ls) {
                nextState = ls.withLastAtBatIndex(newLastAtBatIndex);
            }

            rescheduleIfNeeded(nextState, feed);
            currentState = nextState;

        } catch (RuntimeException e) {
            log.error("Poll error for gamePk={}: {}", gamePk, e.getMessage(), e);
        }
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
