package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;

import java.time.Duration;
import java.util.Map;

/**
 * A game that has not yet started. Polls every 60 seconds.
 * emitEvents is a no-op. Transitions to LiveState when feed reports "Live".
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
    @SuppressWarnings("unchecked")
    public MlbGameState transition(Map<String, Object> feed) {
        var gameData = (Map<String, Object>) feed.get("gameData");
        var status   = (Map<String, Object>) gameData.get("status");
        var state    = (String) status.get("abstractGameState");

        return switch (state) {
            case "Live"  -> new LiveState(gamePk, timecodeFrom(feed), -1);
            default      -> this;
        };
    }

    @Override
    public int emitEvents(Map<String, Object> feed, EventProducer producer) {
        return -1; // no-op: game has not started
    }

    @SuppressWarnings("unchecked")
    private String timecodeFrom(Map<String, Object> feed) {
        var meta = (Map<String, Object>) feed.get("metaData");
        return (String) meta.get("timeStamp");
    }
}
