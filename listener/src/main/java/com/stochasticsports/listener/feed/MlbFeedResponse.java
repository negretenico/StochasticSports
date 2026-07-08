package com.stochasticsports.listener.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Typed representation of the MLB Stats API /game/{pk}/feed/live response.
 * Jackson deserialises directly into this record hierarchy — no Map casting needed.
 * @JsonIgnoreProperties(ignoreUnknown = true) on each record tolerates API fields we don't use.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MlbFeedResponse(MetaData metaData, GameData gameData, LiveData liveData) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MetaData(String timeStamp, @JsonProperty("wait") int waitSeconds) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GameData(Game game, Datetime datetime, Status status, Teams teams) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Game(int pk) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Datetime(String officialDate) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Status(String abstractGameState) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Teams(Team home, Team away) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Team(int id, String name) {}
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LiveData(Plays plays) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Plays(List<Play> allPlays) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Play(About about, Result result, Matchup matchup, Count count) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record About(int atBatIndex, int inning, String halfInning, boolean isComplete) {}

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Result(
                String event, String eventType, String description,
                int homeScore, int awayScore, int rbi, boolean isScoringPlay
            ) {}

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Matchup(Player batter, Player pitcher) {

                @JsonIgnoreProperties(ignoreUnknown = true)
                public record Player(int id, String fullName) {}
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Count(int outs) {}
        }
    }
}
