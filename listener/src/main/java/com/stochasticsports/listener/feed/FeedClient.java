package com.stochasticsports.listener.feed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

/**
 * HTTP adapter for the MLB Stats API live feed endpoint.
 * Uses WebClient (WebFlux) with blocking call — safe on scheduled threads.
 */
@Slf4j
public class FeedClient {

    private final WebClient webClient;

    public FeedClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches the live feed for a game.
     *
     * @param gamePk    MLB game primary key
     * @param timecode  Last-seen timecode in YYYYMMDD_HHMMSS format, or null for first poll
     * @return Raw feed as a nested Map (caller drives state machine)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetch(int gamePk, String timecode) {
        var spec = webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/game/{pk}/feed/live");
                    Optional.ofNullable(timecode)
                            .ifPresent(tc -> builder.queryParam("timecode", tc));
                    return builder.build(gamePk);
                });

        return spec.retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("Failed to fetch feed for gamePk={}: {}", gamePk, e.getMessage()))
                .block();
    }
}
