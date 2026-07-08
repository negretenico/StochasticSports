package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;
import lombok.extern.slf4j.Slf4j;

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

    public GamePoller(
            int gamePk,
            MlbGameState initialState,
            FeedClient feedClient,
            EventProducer producer,
            ScheduledExecutorService scheduler) {
        this.gamePk       = gamePk;
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
        try {
            var feed = feedClient.fetch(gamePk, currentState.lastTimecode());

            // Emit first using currentState so lastAtBatIndex is correct for dedup.
            // Returns the new max atBatIndex after emission (or -1 for non-Live states).
            int newLastAtBatIndex = switch (currentState) {
                case LiveState ls -> ls.emit(feed, producer);
                case PreviewState p -> -1;
                case FinalState f -> -1;
            };

            // Transition, then patch the updated atBatIndex into the next LiveState.
            var nextState = currentState.transition(feed);
            if (nextState instanceof LiveState ls) {
                nextState = ls.withLastAtBatIndex(newLastAtBatIndex);
            }

            boolean typeChanged = !nextState.getClass().equals(currentState.getClass());
            if (typeChanged) {
                future.cancel(false);

                if (!nextState.isTerminal()) {
                    long nextDelaySecs = nextState instanceof LiveState live
                            ? live.pollIntervalFromFeed(feed).toSeconds()
                            : nextState.pollInterval().toSeconds();
                    future = scheduler.scheduleWithFixedDelay(
                            this::poll, nextDelaySecs, nextDelaySecs, TimeUnit.SECONDS);
                    log.info("Transitioned gamePk={} to {} interval={}s",
                            gamePk, nextState.getClass().getSimpleName(), nextDelaySecs);
                } else {
                    log.info("Game gamePk={} reached Final state — poller stopped", gamePk);
                }
            }

            currentState = nextState;

        } catch (org.springframework.web.reactive.function.client.WebClientException e) {
            log.error("HTTP error polling gamePk={}: {}", gamePk, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Poll error for gamePk={}: {}", gamePk, e.getMessage(), e);
        }
    }
}
