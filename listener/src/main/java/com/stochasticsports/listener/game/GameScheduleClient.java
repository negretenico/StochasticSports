package com.stochasticsports.listener.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
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
    public List<Integer> fetchActiveGamePks(String date) {
        var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/schedule")
                        .queryParam("sportId", "1")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(ScheduleResponse.class)
                .doOnError(e -> log.error("Failed to fetch schedule for date={}: {}", date, e.getMessage()))
                .block();

        return Optional.ofNullable(response)
                .map(ScheduleResponse::dates)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(dateEntry -> dateEntry.games().stream())
                .filter(game -> !"Final".equals(game.status().abstractGameState()))
                .map(ScheduleResponse.DateEntry.Game::gamePk)
                .toList();
    }
}
