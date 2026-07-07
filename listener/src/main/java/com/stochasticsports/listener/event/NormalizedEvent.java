package com.stochasticsports.listener.event;

/**
 * Immutable value representing a completed MLB at-bat event ready for Kafka production.
 * eventId is "{gamePk}_{atBatIndex}" — used for tracing only, not as a dedup key.
 */
public record NormalizedEvent(
        String  eventId,
        int     gamePk,
        String  gameDate,
        int     homeTeamId,
        String  homeTeamName,
        int     awayTeamId,
        String  awayTeamName,
        String  abstractGameState,
        int     inning,
        String  halfInning,
        int     outsWhenUp,
        int     homeScore,
        int     awayScore,
        int     batterId,
        String  batterName,
        int     pitcherId,
        String  pitcherName,
        String  resultEvent,
        String  resultEventType,
        String  resultDescription,
        int     rbi,
        boolean isScoringPlay,
        String  ingestedAt
) {}
