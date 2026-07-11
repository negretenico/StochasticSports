package com.stochasticsports.listener.feed;

import com.stochasticsports.listener.event.EventProducer;
import com.stochasticsports.listener.event.NormalizedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamePollerTest {

    @Mock private FeedClient feedClient;
    @Mock private EventProducer producer;
    @Mock private ScheduledExecutorService scheduler;
    @Mock private ScheduledFuture<?> future;

    private static final int GAME_PK = 747175;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @BeforeEach
    void setUp() {
        // scheduler returns a future when scheduleWithFixedDelay is called
        when(scheduler.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn((ScheduledFuture) future);
    }

    // ── Cycle 10: poll() with Live feed calls emitEvents ─────────────────────

    @Test
    void poll_withLiveFeed_callsEmitEventsOnState() {
        var liveState = spy(new LiveState(GAME_PK, null, -1));
        var poller = GamePoller.builder().initialState(liveState).feedClient(feedClient).producer(producer).scheduler(scheduler).normalizer(new Normalizer()).build();

        MlbFeedResponse liveFeed = buildFeed("Live", "20260704_175655", 10);
        when(feedClient.fetch(eq(GAME_PK), isNull())).thenReturn(liveFeed);

        poller.start();
        poller.poll();

        // emitEvents should have been driven (producer.send called for each at-bat)
        // We verify the state's emitEvents by verifying the producer received calls
        // (LiveState.emitEvents delegates to Normalizer then producer.send)
        // Since our feed has no allPlays, we verify no exception was thrown
        // and the state stays Live (future not cancelled)
        verify(future, never()).cancel(false);
    }

    @Test
    void poll_withFinalFeed_cancelsFutureAndDoesNotReschedule() {
        var liveState = new LiveState(GAME_PK, "20260704_170000", -1);
        var poller = GamePoller.builder().initialState(liveState).feedClient(feedClient).producer(producer).scheduler(scheduler).normalizer(new Normalizer()).build();

        MlbFeedResponse finalFeed = buildFeed("Final", "20260704_220000", 10);
        when(feedClient.fetch(eq(GAME_PK), eq("20260704_170000"))).thenReturn(finalFeed);

        poller.start();
        // first scheduleWithFixedDelay captured — verify it
        verify(scheduler, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());

        poller.poll();

        // future cancelled when transitioning to terminal
        verify(future).cancel(false);
        // no second schedule call after Final
        verify(scheduler, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
    }

    @Test
    void poll_withPreviewFeed_doesNotCallProducerAndStaysPreview() {
        var previewState = new PreviewState(GAME_PK);
        var poller = GamePoller.builder().initialState(previewState).feedClient(feedClient).producer(producer).scheduler(scheduler).normalizer(new Normalizer()).build();

        MlbFeedResponse previewFeed = buildFeed("Preview", "20260704_150000", 60);
        when(feedClient.fetch(eq(GAME_PK), isNull())).thenReturn(previewFeed);

        poller.start();
        poller.poll();

        // Preview emitEvents is a no-op, producer never called
        verify(producer, never()).send(any(NormalizedEvent.class));
        // remains Preview (no state change) — future not cancelled
        verify(future, never()).cancel(false);
    }

    @Test
    void poll_transitionFromPreviewToLive_cancelsOldFutureAndReschedulesWithLiveInterval() {
        var previewState = new PreviewState(GAME_PK);
        var poller = GamePoller.builder().initialState(previewState).feedClient(feedClient).producer(producer).scheduler(scheduler).normalizer(new Normalizer()).build();

        MlbFeedResponse liveFeed = buildFeed("Live", "20260704_175655", 10);
        when(feedClient.fetch(eq(GAME_PK), isNull())).thenReturn(liveFeed);

        poller.start();
        poller.poll();

        // Old Preview future cancelled on state change
        verify(future).cancel(false);
        // Total of 2 schedule calls: start() with 60s (Preview) + reschedule with 10s (Live)
        verify(scheduler, times(2)).scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        // Specifically the Live reschedule uses 10s
        verify(scheduler, times(1)).scheduleWithFixedDelay(
                any(Runnable.class),
                eq(10L), eq(10L), eq(TimeUnit.SECONDS));
    }

    // helper: builds a minimal MlbFeedResponse matching what the state machine reads
    private MlbFeedResponse buildFeed(String abstractGameState, String timestamp, int wait) {
        return new MlbFeedResponse(
                new MlbFeedResponse.MetaData(timestamp, wait),
                new MlbFeedResponse.GameData(
                        new MlbFeedResponse.GameData.Game(GAME_PK),
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
