package com.stochasticsports.listener.feed;

import java.time.Duration;

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
    public String lastTimecode() {
        return null;
    }

    @Override
    public MlbGameState transition(MlbFeedResponse feed) {
        return switch (feed.gameData().status().abstractGameState()) {
            case "Live" -> new LiveState(gamePk, feed.metaData().timeStamp(), -1);
            default     -> this;
        };
    }
}
