package com.stochasticsports.listener.game;

import java.util.List;

/**
 * Typed response from the MLB Stats API /schedule endpoint.
 */
public record ScheduleResponse(List<DateEntry> dates) {

    public record DateEntry(List<Game> games) {

        public record Game(int gamePk, GameStatus status) {

            public record GameStatus(String abstractGameState) {}
        }
    }
}
