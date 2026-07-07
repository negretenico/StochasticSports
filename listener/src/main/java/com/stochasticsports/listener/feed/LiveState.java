package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * A game currently in progress. Polls every metaData.wait seconds (or 10s default).
 * Emits normalized at-bat-complete events. Transitions to FinalState when feed reports "Final".
 */
public record LiveState(int gamePk, String lastTimecode, int lastAtBatIndex) implements MlbGameState {

    @Override
    public Duration pollInterval() {
        return Duration.ofSeconds(10);
    }

    /**
     * Returns the poll interval derived from the feed's metaData.wait, defaulting to 10s.
     */
    @SuppressWarnings("unchecked")
    public Duration pollIntervalFromFeed(Map<String, Object> feed) {
        return Optional.ofNullable((Map<String, Object>) feed.get("metaData"))
                .map(meta -> (Number) meta.get("wait"))
                .map(wait -> Duration.ofSeconds(wait.longValue()))
                .orElse(Duration.ofSeconds(10));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MlbGameState transition(Map<String, Object> feed) {
        var gameData = (Map<String, Object>) feed.get("gameData");
        var status   = (Map<String, Object>) gameData.get("status");
        var state    = (String) status.get("abstractGameState");
        var newCode  = timecodeFrom(feed);

        return switch (state) {
            case "Final" -> new FinalState(gamePk);
            default      -> new LiveState(gamePk, newCode, lastAtBatIndex);
        };
    }

    @Override
    public void emitEvents(Map<String, Object> feed, EventProducer producer) {
        Normalizer.normalize(feed, lastAtBatIndex)
                .forEach(producer::send);
    }

    @SuppressWarnings("unchecked")
    private String timecodeFrom(Map<String, Object> feed) {
        var meta = (Map<String, Object>) feed.get("metaData");
        return (String) meta.get("timeStamp");
    }
}
