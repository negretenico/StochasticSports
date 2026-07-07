package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.NormalizedEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure function: raw feed Map -> List<NormalizedEvent>.
 * No Spring dependencies. Only emits completed at-bats with atBatIndex > lastAtBatIndex.
 */
public final class Normalizer {

    private Normalizer() {}

    /**
     * @param feed          Raw feed response from MLB Stats API /game/{pk}/feed/live
     * @param lastAtBatIndex Highest at-bat index already emitted (-1 to emit all)
     * @return Normalized events for at-bats with index strictly greater than lastAtBatIndex
     */
    @SuppressWarnings("unchecked")
    public static List<NormalizedEvent> normalize(Map<String, Object> feed, int lastAtBatIndex) {
        var gameData    = (Map<String, Object>) feed.get("gameData");
        var game        = (Map<String, Object>) gameData.get("game");
        var datetime    = (Map<String, Object>) gameData.get("datetime");
        var status      = (Map<String, Object>) gameData.get("status");
        var teams       = (Map<String, Object>) gameData.get("teams");
        var home        = (Map<String, Object>) teams.get("home");
        var away        = (Map<String, Object>) teams.get("away");
        var liveData    = (Map<String, Object>) feed.get("liveData");
        var plays       = (Map<String, Object>) liveData.get("plays");
        var allPlays    = (List<Map<String, Object>>) plays.get("allPlays");

        int    gamePk            = ((Number) game.get("pk")).intValue();
        String gameDate          = (String) datetime.get("officialDate");
        String abstractGameState = (String) status.get("abstractGameState");
        int    homeTeamId        = ((Number) home.get("id")).intValue();
        String homeTeamName      = (String) home.get("name");
        int    awayTeamId        = ((Number) away.get("id")).intValue();
        String awayTeamName      = (String) away.get("name");
        String ingestedAt        = Instant.now().toString();

        return allPlays.stream()
                .filter(play -> isCompleted(play) && atBatIndexOf(play) > lastAtBatIndex)
                .map(play -> toEvent(
                        play, gamePk, gameDate, abstractGameState,
                        homeTeamId, homeTeamName, awayTeamId, awayTeamName, ingestedAt))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static boolean isCompleted(Map<String, Object> play) {
        var about = (Map<String, Object>) play.get("about");
        return Boolean.TRUE.equals(about.get("isComplete"));
    }

    @SuppressWarnings("unchecked")
    private static int atBatIndexOf(Map<String, Object> play) {
        var about = (Map<String, Object>) play.get("about");
        return ((Number) about.get("atBatIndex")).intValue();
    }

    @SuppressWarnings("unchecked")
    private static NormalizedEvent toEvent(
            Map<String, Object> play,
            int gamePk,
            String gameDate,
            String abstractGameState,
            int homeTeamId,
            String homeTeamName,
            int awayTeamId,
            String awayTeamName,
            String ingestedAt) {

        var result  = (Map<String, Object>) play.get("result");
        var about   = (Map<String, Object>) play.get("about");
        var matchup = (Map<String, Object>) play.get("matchup");
        var count   = (Map<String, Object>) play.get("count");
        var batter  = (Map<String, Object>) matchup.get("batter");
        var pitcher = (Map<String, Object>) matchup.get("pitcher");

        int    atBatIndex        = ((Number) about.get("atBatIndex")).intValue();
        int    inning            = ((Number) about.get("inning")).intValue();
        String halfInning        = (String) about.get("halfInning");
        int    outsWhenUp        = ((Number) count.get("outs")).intValue();
        int    homeScore         = ((Number) result.get("homeScore")).intValue();
        int    awayScore         = ((Number) result.get("awayScore")).intValue();
        int    batterId          = ((Number) batter.get("id")).intValue();
        String batterName        = (String) batter.get("fullName");
        int    pitcherId         = ((Number) pitcher.get("id")).intValue();
        String pitcherName       = (String) pitcher.get("fullName");
        String resultEvent       = (String) result.get("event");
        String resultEventType   = (String) result.get("eventType");
        String resultDescription = (String) result.get("description");
        int    rbi               = ((Number) result.get("rbi")).intValue();
        boolean isScoringPlay    = Boolean.TRUE.equals(result.get("isScoringPlay"));

        return new NormalizedEvent(
                gamePk + "_" + atBatIndex,
                gamePk,
                gameDate,
                homeTeamId,
                homeTeamName,
                awayTeamId,
                awayTeamName,
                abstractGameState,
                inning,
                halfInning,
                outsWhenUp,
                homeScore,
                awayScore,
                batterId,
                batterName,
                pitcherId,
                pitcherName,
                resultEvent,
                resultEventType,
                resultDescription,
                rbi,
                isScoringPlay,
                ingestedAt
        );
    }
}
