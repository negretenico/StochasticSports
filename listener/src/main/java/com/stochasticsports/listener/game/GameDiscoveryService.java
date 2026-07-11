package com.stochasticsports.listener.game;

import com.stochasticsports.listener.config.ListenerProperties;
import com.stochasticsports.listener.feed.GamePollerFactory;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers active games for today and starts a GamePoller for each new gamePk.
 * No Spring annotations — scheduled externally via ListenerConfig.
 * Tracks already-started pollers so the same game is never double-scheduled.
 */
@Slf4j
public class GameDiscoveryService {

    private final GameScheduleClient scheduleClient;
    private final GamePollerFactory  pollerFactory;
    private final ListenerProperties props;

    private final Set<Integer> managedGames = ConcurrentHashMap.newKeySet();

    public GameDiscoveryService(
            GameScheduleClient scheduleClient,
            GamePollerFactory pollerFactory,
            ListenerProperties props) {
        this.scheduleClient = scheduleClient;
        this.pollerFactory  = pollerFactory;
        this.props          = props;
    }

    public void discoverAndRegister() {
        String date = resolveDate();
        log.info("Discovering active games for date={}", date);

        scheduleClient.fetchActiveGamePks(date).stream()
                .filter(pk -> !managedGames.contains(pk))
                .forEach(pk -> {
                    managedGames.add(pk);
                    pollerFactory.create(pk).start();
                    log.info("Started GamePoller for gamePk={}", pk);
                });
    }

    private String resolveDate() {
        return Optional.ofNullable(props.pollDate())
                .filter(s -> !s.isBlank())
                .orElse(LocalDate.now().toString());
    }
}
