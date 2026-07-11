package com.stochasticsports.listener.feed;

import java.time.Duration;
import java.util.Optional;

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
    public Optional<String> lastTimecode() {
        return Optional.empty();
    }

    @Override
    public MlbGameState transition(MlbFeedResponse feed) {
        return this;
    }
}
