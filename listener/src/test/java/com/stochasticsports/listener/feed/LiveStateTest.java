package com.stochasticsports.listener.feed;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LiveStateTest {

    // ── Cycle 3: LiveState.transition ─────────────────────────────────────────

    @Test
    void transition_updatesTimecode_whenStateRemainsLive() {
        var state = new LiveState(747175, "20260704_170000", 0);
        var feed = feedWithState("Live", "20260704_175655", 10);

        var next = state.transition(feed);

        assertThat(next).isInstanceOf(LiveState.class);
        assertThat(next.gamePk()).isEqualTo(747175);
        assertThat(((LiveState) next).lastTimecode()).isEqualTo("20260704_175655");
    }

    @Test
    void transition_becomesFinal_whenGameStateIsFinal() {
        var state = new LiveState(747175, "20260704_170000", 5);
        var feed = feedWithState("Final", "20260704_220000", 10);

        var next = state.transition(feed);

        assertThat(next).isInstanceOf(FinalState.class);
        assertThat(next.gamePk()).isEqualTo(747175);
    }

    // ── Cycle 4: LiveState.pollInterval ───────────────────────────────────────

    @Test
    void pollInterval_usesMetaDataWait_whenPresent() {
        var state = new LiveState(747175, "20260704_170000", 0);
        var feed = feedWithState("Live", "20260704_175655", 7);

        // pollInterval comes from the feed's metaData.wait
        assertThat(state.pollIntervalFromFeed(feed)).isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    void pollInterval_defaultsTo10Seconds_whenMetaDataWaitAbsent() {
        var state = new LiveState(747175, "20260704_170000", 0);
        var feed = Map.<String, Object>of(
                "gameData", Map.of(
                        "status", Map.of("abstractGameState", "Live")
                ),
                "metaData", Map.of("timeStamp", "20260704_175655")
                // no "wait" key
        );

        assertThat(state.pollIntervalFromFeed(feed)).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void pollInterval_baseMethod_returns10Seconds() {
        // pollInterval() without feed uses stored default
        var state = new LiveState(747175, "20260704_170000", 0);
        assertThat(state.pollInterval()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void isTerminal_isFalse() {
        assertThat(new LiveState(747175, null, 0).isTerminal()).isFalse();
    }

    // helper
    @SuppressWarnings("unchecked")
    private Map<String, Object> feedWithState(String abstractGameState, String timestamp, int wait) {
        return Map.of(
                "gameData", Map.of(
                        "status", Map.of("abstractGameState", abstractGameState)
                ),
                "metaData", Map.of(
                        "timeStamp", timestamp,
                        "wait", wait
                )
        );
    }
}
