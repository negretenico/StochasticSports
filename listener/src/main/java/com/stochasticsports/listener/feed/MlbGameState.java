package com.stochasticsports.listener.feed;

import java.time.Duration;

/**
 * Sealed interface representing the state machine for a single MLB game.
 * Transitions: Preview -> Live -> Final (terminal).
 * All implementations are pure value types (records); no Spring dependencies.
 */
public sealed interface MlbGameState permits PreviewState, LiveState, FinalState {

    int gamePk();

    /** Base poll interval for scheduling. For LiveState, prefer pollIntervalFromFeed(). */
    Duration pollInterval();

    boolean isTerminal();

    /**
     * Returns the last timecode used in this state's feed fetch,
     * or null if not applicable (Preview).
     */
    String lastTimecode();

    /**
     * Computes the next state from the typed feed response.
     * Returns the same state instance (or updated version) based on abstractGameState.
     */
    MlbGameState transition(MlbFeedResponse feed);
}
