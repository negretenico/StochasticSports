package com.stochasticsports.listener.feed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
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
     * @return Typed feed response deserialized by Jackson
     */
    public MlbFeedResponse fetch(int gamePk, String timecode) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/game/{pk}/feed/live");
                    Optional.ofNullable(timecode)
                            .ifPresent(tc -> builder.queryParam("timecode", tc));
                    return builder.build(gamePk);
                })
                .retrieve()
                .bodyToMono(MlbFeedResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(60))
                        .filter(e -> !(e instanceof WebClientResponseException wce) || wce.getStatusCode().is5xxServerError())
                        .doBeforeRetry(signal -> log.warn("Retrying gamePk={} attempt={}: {}",
                                gamePk, signal.totalRetries() + 1, signal.failure().getMessage())))
                .block();
    }
}
