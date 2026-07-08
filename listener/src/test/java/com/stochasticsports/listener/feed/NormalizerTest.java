package com.stochasticsports.listener.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stochasticsports.listener.event.NormalizedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizerTest {

    private MlbFeedResponse feed;

    @BeforeEach
    void loadFixture() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/fixtures/feed_live_response.json")) {
            feed = mapper.readValue(is, MlbFeedResponse.class);
        }
    }

    // ── Cycle 1: NormalizedEvent can be constructed ──────────────────────────

    @Test
    void normalizedEvent_canBeConstructedWithAllFields() {
        var event = new NormalizedEvent(
                "747175_0",
                747175,
                new NormalizedEvent.GameInfo(
                        "2026-07-04",
                        111,
                        "Boston Red Sox",
                        147,
                        "New York Yankees",
                        "Live",
                        1,
                        "top",
                        0
                ),
                new NormalizedEvent.Score(0, 0),
                new NormalizedEvent.Matchup(
                        592450,
                        "Aaron Judge",
                        641154,
                        "Brayan Bello"
                ),
                new NormalizedEvent.AtBatResult(
                        "Single",
                        "single",
                        "Aaron Judge singles on a line drive to left fielder Masataka Yoshida.",
                        0,
                        false
                ),
                "2026-07-04T17:14:23Z"
        );

        assertThat(event.eventId()).isEqualTo("747175_0");
        assertThat(event.gamePk()).isEqualTo(747175);
        assertThat(event.game().gameDate()).isEqualTo("2026-07-04");
        assertThat(event.game().homeTeamId()).isEqualTo(111);
        assertThat(event.game().homeTeamName()).isEqualTo("Boston Red Sox");
        assertThat(event.game().awayTeamId()).isEqualTo(147);
        assertThat(event.game().awayTeamName()).isEqualTo("New York Yankees");
        assertThat(event.game().abstractGameState()).isEqualTo("Live");
        assertThat(event.game().inning()).isEqualTo(1);
        assertThat(event.game().halfInning()).isEqualTo("top");
        assertThat(event.game().outsWhenUp()).isEqualTo(0);
        assertThat(event.score().home()).isEqualTo(0);
        assertThat(event.score().away()).isEqualTo(0);
        assertThat(event.matchup().batterId()).isEqualTo(592450);
        assertThat(event.matchup().batterName()).isEqualTo("Aaron Judge");
        assertThat(event.matchup().pitcherId()).isEqualTo(641154);
        assertThat(event.matchup().pitcherName()).isEqualTo("Brayan Bello");
        assertThat(event.result().event()).isEqualTo("Single");
        assertThat(event.result().eventType()).isEqualTo("single");
        assertThat(event.result().rbi()).isEqualTo(0);
        assertThat(event.result().isScoringPlay()).isFalse();
    }

    // ── Cycle 5: Normalizer.normalize(feed, -1) returns events from fixture ──

    @Test
    void normalize_withNoLastIndex_returnsAllCompletedAtBats() {
        var events = Normalizer.normalize(feed, -1);

        assertThat(events).hasSize(2);

        NormalizedEvent first = events.get(0);
        assertThat(first.gamePk()).isEqualTo(747175);
        assertThat(first.game().inning()).isEqualTo(1);
        assertThat(first.result().event()).isEqualTo("Single");
        assertThat(first.score().home()).isEqualTo(0);
        assertThat(first.score().away()).isEqualTo(0);
        assertThat(first.game().homeTeamId()).isEqualTo(111);
        assertThat(first.game().homeTeamName()).isEqualTo("Boston Red Sox");
        assertThat(first.game().awayTeamId()).isEqualTo(147);
        assertThat(first.game().awayTeamName()).isEqualTo("New York Yankees");
        assertThat(first.eventId()).isEqualTo("747175_0");

        NormalizedEvent second = events.get(1);
        assertThat(second.gamePk()).isEqualTo(747175);
        assertThat(second.game().inning()).isEqualTo(1);
        assertThat(second.result().event()).isEqualTo("Home Run");
        assertThat(second.score().home()).isEqualTo(0);
        assertThat(second.score().away()).isEqualTo(2);
        assertThat(second.result().isScoringPlay()).isTrue();
        assertThat(second.result().rbi()).isEqualTo(2);
        assertThat(second.eventId()).isEqualTo("747175_1");
    }

    // ── Cycle 6: Normalizer.normalize(feed, N) excludes at-bats where atBatIndex <= N ──

    @Test
    void normalize_withLastAtBatIndex_excludesPreviouslySeen() {
        List<NormalizedEvent> events = Normalizer.normalize(feed, 0);

        // atBatIndex 0 was already seen — only index 1 should be returned
        assertThat(events).hasSize(1);
        assertThat(events.get(0).result().event()).isEqualTo("Home Run");
        assertThat(events.get(0).eventId()).isEqualTo("747175_1");
    }

    @Test
    void normalize_withLastAtBatIndexAtMax_returnsEmpty() {
        List<NormalizedEvent> events = Normalizer.normalize(feed, 1);

        assertThat(events).isEmpty();
    }
}
