package com.stochasticsports.listener.feed;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

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
        // wait = 0 simulates the field being absent (int defaults to 0)
        var feed = feedWithState("Live", "20260704_175655", 0);

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
    private MlbFeedResponse feedWithState(String abstractGameState, String timestamp, int wait) {
        return new MlbFeedResponse(
                new MlbFeedResponse.MetaData(timestamp, wait),
                new MlbFeedResponse.GameData(
                        new MlbFeedResponse.GameData.Game(747175),
                        new MlbFeedResponse.GameData.Datetime("2026-07-04"),
                        new MlbFeedResponse.GameData.Status(abstractGameState),
                        new MlbFeedResponse.GameData.Teams(
                                new MlbFeedResponse.GameData.Teams.Team(111, "Boston Red Sox"),
                                new MlbFeedResponse.GameData.Teams.Team(147, "New York Yankees")
                        )
                ),
                new MlbFeedResponse.LiveData(
                        new MlbFeedResponse.LiveData.Plays(List.of())
                )
        );
    }
}
