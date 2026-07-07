package com.stochasticsports.listener.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stochasticsports.listener.event.NormalizedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizerTest {

    private Map<String, Object> feed;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void loadFixture() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/fixtures/feed_live_response.json")) {
            feed = mapper.readValue(is, Map.class);
        }
    }

    // ── Cycle 1: NormalizedEvent can be constructed ──────────────────────────

    @Test
    void normalizedEvent_canBeConstructedWithAllFields() {
        var event = new NormalizedEvent(
                "747175_0",
                747175,
                "2026-07-04",
                111,
                "Boston Red Sox",
                147,
                "New York Yankees",
                "Live",
                1,
                "top",
                0,
                0,
                0,
                592450,
                "Aaron Judge",
                641154,
                "Brayan Bello",
                "Single",
                "single",
                "Aaron Judge singles on a line drive to left fielder Masataka Yoshida.",
                0,
                false,
                "2026-07-04T17:14:23Z"
        );

        assertThat(event.eventId()).isEqualTo("747175_0");
        assertThat(event.gamePk()).isEqualTo(747175);
        assertThat(event.gameDate()).isEqualTo("2026-07-04");
        assertThat(event.homeTeamId()).isEqualTo(111);
        assertThat(event.homeTeamName()).isEqualTo("Boston Red Sox");
        assertThat(event.awayTeamId()).isEqualTo(147);
        assertThat(event.awayTeamName()).isEqualTo("New York Yankees");
        assertThat(event.abstractGameState()).isEqualTo("Live");
        assertThat(event.inning()).isEqualTo(1);
        assertThat(event.halfInning()).isEqualTo("top");
        assertThat(event.outsWhenUp()).isEqualTo(0);
        assertThat(event.homeScore()).isEqualTo(0);
        assertThat(event.awayScore()).isEqualTo(0);
        assertThat(event.batterId()).isEqualTo(592450);
        assertThat(event.batterName()).isEqualTo("Aaron Judge");
        assertThat(event.pitcherId()).isEqualTo(641154);
        assertThat(event.pitcherName()).isEqualTo("Brayan Bello");
        assertThat(event.resultEvent()).isEqualTo("Single");
        assertThat(event.resultEventType()).isEqualTo("single");
        assertThat(event.rbi()).isEqualTo(0);
        assertThat(event.isScoringPlay()).isFalse();
    }

    // ── Cycle 5: Normalizer.normalize(feed, -1) returns events from fixture ──

    @Test
    void normalize_withNoLastIndex_returnsAllCompletedAtBats() {
        var events = Normalizer.normalize(feed, -1);

        assertThat(events).hasSize(2);

        NormalizedEvent first = events.get(0);
        assertThat(first.gamePk()).isEqualTo(747175);
        assertThat(first.inning()).isEqualTo(1);
        assertThat(first.resultEvent()).isEqualTo("Single");
        assertThat(first.homeScore()).isEqualTo(0);
        assertThat(first.awayScore()).isEqualTo(0);
        assertThat(first.homeTeamId()).isEqualTo(111);
        assertThat(first.homeTeamName()).isEqualTo("Boston Red Sox");
        assertThat(first.awayTeamId()).isEqualTo(147);
        assertThat(first.awayTeamName()).isEqualTo("New York Yankees");
        assertThat(first.eventId()).isEqualTo("747175_0");

        NormalizedEvent second = events.get(1);
        assertThat(second.gamePk()).isEqualTo(747175);
        assertThat(second.inning()).isEqualTo(1);
        assertThat(second.resultEvent()).isEqualTo("Home Run");
        assertThat(second.homeScore()).isEqualTo(0);
        assertThat(second.awayScore()).isEqualTo(2);
        assertThat(second.isScoringPlay()).isTrue();
        assertThat(second.rbi()).isEqualTo(2);
        assertThat(second.eventId()).isEqualTo("747175_1");
    }

    // ── Cycle 6: Normalizer.normalize(feed, N) excludes at-bats where atBatIndex <= N ──

    @Test
    void normalize_withLastAtBatIndex_excludesPreviouslySeen() {
        List<NormalizedEvent> events = Normalizer.normalize(feed, 0);

        // atBatIndex 0 was already seen — only index 1 should be returned
        assertThat(events).hasSize(1);
        assertThat(events.get(0).resultEvent()).isEqualTo("Home Run");
        assertThat(events.get(0).eventId()).isEqualTo("747175_1");
    }

    @Test
    void normalize_withLastAtBatIndexAtMax_returnsEmpty() {
        List<NormalizedEvent> events = Normalizer.normalize(feed, 1);

        assertThat(events).isEmpty();
    }
}
