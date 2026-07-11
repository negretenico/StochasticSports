package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.NormalizedEvent;

import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Pure function: MlbFeedResponse × lastAtBatIndex → List<NormalizedEvent>.
 * Exposed as a BiFunction so it can be passed, composed, or replaced in tests.
 * Only emits completed at-bats with atBatIndex > lastAtBatIndex.
 */
public final class Normalizer implements BiFunction<MlbFeedResponse, Integer, List<NormalizedEvent>> {

    @Override
    public List<NormalizedEvent> apply(MlbFeedResponse feed, Integer lastAtBatIndex) {
        var gd         = feed.gameData();
        var ingestedAt = Instant.now().toString();

        return feed.liveData().plays().allPlays().stream()
                .filter(play -> play.about().isComplete() && play.about().atBatIndex() > lastAtBatIndex)
                .map(play -> toEvent(play, gd, ingestedAt))
                .toList();
    }

    private NormalizedEvent toEvent(
            MlbFeedResponse.LiveData.Play play,
            MlbFeedResponse.GameData gd,
            String ingestedAt) {

        var about   = play.about();
        var result  = play.result();
        var matchup = play.matchup();

        return NormalizedEvent.builder()
                .eventId(gd.game().pk() + "_" + about.atBatIndex())
                .gamePk(gd.game().pk())
                .game(NormalizedEvent.GameInfo.builder()
                        .gameDate(gd.datetime().officialDate())
                        .homeTeamId(gd.teams().home().id())
                        .homeTeamName(gd.teams().home().name())
                        .awayTeamId(gd.teams().away().id())
                        .awayTeamName(gd.teams().away().name())
                        .abstractGameState(gd.status().abstractGameState())
                        .inning(about.inning())
                        .halfInning(about.halfInning())
                        .outsWhenUp(play.count().outs())
                        .build())
                .score(new NormalizedEvent.Score(result.homeScore(), result.awayScore()))
                .matchup(new NormalizedEvent.Matchup(
                        matchup.batter().id(),
                        matchup.batter().fullName(),
                        matchup.pitcher().id(),
                        matchup.pitcher().fullName()))
                .result(new NormalizedEvent.AtBatResult(
                        result.event(),
                        result.eventType(),
                        result.description(),
                        result.rbi(),
                        result.isScoringPlay()))
                .ingestedAt(ingestedAt)
                .build();
    }
}
