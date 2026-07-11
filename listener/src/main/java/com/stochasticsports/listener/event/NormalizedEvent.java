package com.stochasticsports.listener.event;

import lombok.Builder;

/**
 * Immutable value representing a completed MLB at-bat event ready for Kafka production.
 * eventId is "{gamePk}_{atBatIndex}" — used for tracing only, not as a dedup key.
 */
@Builder
public record NormalizedEvent(
        String     eventId,
        int        gamePk,
        GameInfo   game,
        Score      score,
        Matchup    matchup,
        AtBatResult result,
        String     ingestedAt
) {
    @Builder
    public record GameInfo(
            String gameDate,
            int    homeTeamId,
            String homeTeamName,
            int    awayTeamId,
            String awayTeamName,
            String abstractGameState,
            int    inning,
            String halfInning,
            int    outsWhenUp
    ) {}

    public record Score(int home, int away) {}

    public record Matchup(
            int    batterId,
            String batterName,
            int    pitcherId,
            String pitcherName
    ) {}

    public record AtBatResult(
            String  event,
            String  eventType,
            String  description,
            int     rbi,
            boolean isScoringPlay
    ) {}
}
