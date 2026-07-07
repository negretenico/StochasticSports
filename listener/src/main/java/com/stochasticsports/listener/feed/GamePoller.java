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
            var feed      = feedClient.fetch(gamePk, currentState.lastTimecode());
            var nextState = currentState.transition(feed);

            boolean typeChanged = !nextState.getClass().equals(currentState.getClass());
            currentState = nextState;

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

            currentState.emitEvents(feed, producer);

        } catch (Exception e) {
            log.error("Poll error for gamePk={}: {}", gamePk, e.getMessage(), e);
        }
    }
}
