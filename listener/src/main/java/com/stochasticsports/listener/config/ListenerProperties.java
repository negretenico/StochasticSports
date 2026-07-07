package com.stochasticsports.listener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the listener module.
 * Bound from the "listener" prefix in application.yaml.
 */
@ConfigurationProperties(prefix = "listener")
public record ListenerProperties(
        String mlbApiBaseUrl,
        String pollDate,
        int    discoveryIntervalSeconds,
        int    defaultLivePollSeconds,
        int    previewPollSeconds
) {}
