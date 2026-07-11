package com.stochasticsports.listener.config;

import com.stochasticsports.listener.feed.FeedClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Infrastructure wiring for the listener module: HTTP client, feed adapter, scheduler.
 * Game-domain beans live in GameConfig.
 */
@Configuration
@EnableConfigurationProperties(ListenerProperties.class)
public class ListenerConfig {

    @Bean
    public WebClient mlbWebClient(ListenerProperties props) {
        return WebClient.builder()
                .baseUrl(props.mlbApiBaseUrl())
                .build();
    }

    @Bean
    public FeedClient feedClient(WebClient mlbWebClient) {
        return new FeedClient(mlbWebClient);
    }

    @Bean
    public ScheduledExecutorService gamePollerScheduler() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }
}
