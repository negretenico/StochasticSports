package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.NormalizedEvent;

import java.time.Instant;
import java.util.List;

/**
 * Pure function: typed MlbFeedResponse -> List<NormalizedEvent>.
 * No Spring dependencies. Only emits completed at-bats with atBatIndex > lastAtBatIndex.
 */
public final class Normalizer {

    private Normalizer() {}

    /**
     * @param feed           Typed feed response from MLB Stats API /game/{pk}/feed/live
     * @param lastAtBatIndex Highest at-bat index already emitted (-1 to emit all)
     * @return Normalized events for at-bats with index strictly greater than lastAtBatIndex
     */
    public static List<NormalizedEvent> normalize(MlbFeedResponse feed, int lastAtBatIndex) {
        var gd         = feed.gameData();
        var ingestedAt = Instant.now().toString();

        return feed.liveData().plays().allPlays().stream()
                .filter(play -> play.about().isComplete() && play.about().atBatIndex() > lastAtBatIndex)
                .map(play -> toEvent(play, gd, ingestedAt))
                .toList();
    }

    private static NormalizedEvent toEvent(
            MlbFeedResponse.LiveData.Play play,
            MlbFeedResponse.GameData gd,
            String ingestedAt) {

        var about   = play.about();
        var result  = play.result();
        var matchup = play.matchup();

        return new NormalizedEvent(
                gd.game().pk() + "_" + about.atBatIndex(),
                gd.game().pk(),
                new NormalizedEvent.GameInfo(
                        gd.datetime().officialDate(),
                        gd.teams().home().id(),
                        gd.teams().home().name(),
                        gd.teams().away().id(),
                        gd.teams().away().name(),
                        gd.status().abstractGameState(),
                        about.inning(),
                        about.halfInning(),
                        play.count().outs()
                ),
                new NormalizedEvent.Score(result.homeScore(), result.awayScore()),
                new NormalizedEvent.Matchup(
                        matchup.batter().id(),
                        matchup.batter().fullName(),
                        matchup.pitcher().id(),
                        matchup.pitcher().fullName()
                ),
                new NormalizedEvent.AtBatResult(
                        result.event(),
                        result.eventType(),
                        result.description(),
                        result.rbi(),
                        result.isScoringPlay()
                ),
                ingestedAt
        );
    }
}
