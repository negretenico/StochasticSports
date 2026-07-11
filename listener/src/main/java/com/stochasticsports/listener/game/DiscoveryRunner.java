package com.stochasticsports.listener.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs game discovery immediately on startup, then re-schedules it every intervalSeconds.
 * Implements ApplicationRunner so Spring invokes it after the context is fully loaded.
 */
@Slf4j
public class DiscoveryRunner implements ApplicationRunner {

    private final GameDiscoveryService discoveryService;
    private final ScheduledExecutorService scheduler;
    private final int intervalSeconds;

    public DiscoveryRunner(
            GameDiscoveryService discoveryService,
            ScheduledExecutorService scheduler,
            int intervalSeconds) {
        this.discoveryService = discoveryService;
        this.scheduler        = scheduler;
        this.intervalSeconds  = intervalSeconds;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting game discovery — interval={}s", intervalSeconds);
        discoveryService.discoverAndRegister();
        scheduler.scheduleWithFixedDelay(
                discoveryService::discoverAndRegister,
                intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }
}
