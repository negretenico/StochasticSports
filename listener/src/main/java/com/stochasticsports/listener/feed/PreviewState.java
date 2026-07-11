package com.stochasticsports.listener.feed;

import java.time.Duration;
import java.util.Optional;

/**
 * A game that has not yet started. Polls every 60 seconds.
 * Transitions to LiveState when feed reports "Live".
 */
public record PreviewState(int gamePk) implements MlbGameState {

    @Override
    public Duration pollInterval() {
        return Duration.ofSeconds(60);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public Optional<String> lastTimecode() {
        return Optional.empty();
    }

    @Override
    public MlbGameState transition(MlbFeedResponse feed) {
        return switch (feed.gamePhase()) {
            case "Live" -> new LiveState(gamePk, feed.timecode(), -1);
            default     -> this;
        };
    }
}
