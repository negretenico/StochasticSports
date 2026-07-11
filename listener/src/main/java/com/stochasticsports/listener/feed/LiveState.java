package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;

import java.time.Duration;
import java.util.Optional;

/**
 * A game currently in progress. Polls every metaData.wait seconds (or 10s default).
 * Emits normalized at-bat-complete events. Transitions to FinalState when feed reports "Final".
 */
public record LiveState(int gamePk, String timecode, int lastAtBatIndex) implements MlbGameState {

    @Override
    public Optional<String> lastTimecode() {
        return Optional.ofNullable(timecode);
    }

    @Override
    public Duration pollInterval() {
        return Duration.ofSeconds(10);
    }

    /**
     * Returns the poll interval derived from the feed's metaData.wait, defaulting to 10s.
     */
    public Duration pollIntervalFromFeed(MlbFeedResponse feed) {
        return Duration.ofSeconds(feed.metaData().waitSeconds() > 0 ? feed.metaData().waitSeconds() : 10);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public MlbGameState transition(MlbFeedResponse feed) {
        return switch (feed.gamePhase()) {
            case "Final" -> new FinalState(gamePk);
            default      -> new LiveState(gamePk, feed.timecode(), lastAtBatIndex);
        };
    }

    /**
     * Emits normalized events from the feed to the producer.
     * Returns the new max atBatIndex after emission.
     */
    public int emit(MlbFeedResponse feed, EventProducer producer) {
        var events = Normalizer.normalize(feed, lastAtBatIndex);
        events.forEach(producer::send);
        return events.stream()
                .mapToInt(e -> Integer.parseInt(e.eventId().substring(e.eventId().lastIndexOf('_') + 1)))
                .max()
                .orElse(lastAtBatIndex);
    }

    /** Returns a new LiveState with the given lastAtBatIndex, preserving other fields. */
    public LiveState withLastAtBatIndex(int index) {
        return new LiveState(gamePk, timecode, index);
    }
}
