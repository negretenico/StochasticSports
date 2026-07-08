package com.stochasticsports.listener.feed;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PreviewStateTest {

    // ── Cycle 2: PreviewState.transition ──────────────────────────────────────

    @Test
    void transition_remainsPreview_whenGameStateIsStillPreview() {
        var state = new PreviewState(747175);
        var feed = feedWithState("Preview");

        var next = state.transition(feed);

        assertThat(next).isInstanceOf(PreviewState.class);
        assertThat(next.gamePk()).isEqualTo(747175);
    }

    @Test
    void transition_becomesLive_whenGameStateIsLive() {
        var state = new PreviewState(747175);
        var feed = feedWithState("Live");

        var next = state.transition(feed);

        assertThat(next).isInstanceOf(LiveState.class);
        assertThat(next.gamePk()).isEqualTo(747175);
    }

    @Test
    void pollInterval_is60Seconds() {
        var state = new PreviewState(747175);
        assertThat(state.pollInterval()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void isTerminal_isFalse() {
        assertThat(new PreviewState(747175).isTerminal()).isFalse();
    }

    @Test
    void lastTimecode_isNull() {
        assertThat(new PreviewState(747175).lastTimecode()).isNull();
    }

    // helper
    private MlbFeedResponse feedWithState(String abstractGameState) {
        return new MlbFeedResponse(
                new MlbFeedResponse.MetaData("20260704_170000", 10),
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
