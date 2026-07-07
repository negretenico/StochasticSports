package com.stochasticsports.listener.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP adapter for the MLB Stats API schedule endpoint.
 * Returns gamePks for games that are not yet Final (Live or Preview).
 */
@Slf4j
public class GameScheduleClient {

    private final WebClient webClient;

    public GameScheduleClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * @param date ISO date string YYYY-MM-DD
     * @return gamePks where abstractGameState != "Final"
     */
    @SuppressWarnings("unchecked")
    public List<Integer> fetchActiveGamePks(String date) {
        Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/schedule")
                        .queryParam("sportId", "1")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("Failed to fetch schedule for date={}: {}", date, e.getMessage()))
                .block();

        return Optional.ofNullable(response)
                .map(r -> (List<Map<String, Object>>) r.get("dates"))
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(dateEntry -> ((List<Map<String, Object>>) dateEntry.get("games")).stream())
                .filter(game -> !"Final".equals(abstractGameState(game)))
                .map(game -> ((Number) game.get("gamePk")).intValue())
                .toList();
    }

    @SuppressWarnings("unchecked")
    private String abstractGameState(Map<String, Object> game) {
        return Optional.ofNullable((Map<String, Object>) game.get("status"))
                .map(s -> (String) s.get("abstractGameState"))
                .orElse("Unknown");
    }
}
