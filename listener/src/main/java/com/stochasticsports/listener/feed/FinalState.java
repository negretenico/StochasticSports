package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;

import java.time.Duration;
import java.util.Map;

/**
 * A completed game. Terminal state — never re-scheduled.
 */
public record FinalState(int gamePk) implements MlbGameState {

    @Override
    public Duration pollInterval() {
        return Duration.ZERO;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String lastTimecode() {
        return null;
    }

    @Override
    public MlbGameState transition(Map<String, Object> feed) {
        return this;
    }

    @Override
    public void emitEvents(Map<String, Object> feed, EventProducer producer) {
        // no-op: game is over
    }
}
