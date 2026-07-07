package com.stochasticsports.listener.game;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameScheduleClientTest {

    private MockWebServer server;
    private GameScheduleClient client;
    private String fixtureBody;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        client = new GameScheduleClient(webClient);

        try (InputStream is = getClass().getResourceAsStream("/fixtures/schedule_response.json")) {
            fixtureBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ── Cycle 9: fetchActiveGamePks returns only non-Final games ─────────────

    @Test
    void fetchActiveGamePks_returnsLiveAndPreviewOnly() {
        server.enqueue(new MockResponse()
                .setBody(fixtureBody)
                .addHeader("Content-Type", "application/json"));

        List<Integer> pks = client.fetchActiveGamePks("2026-07-04");

        // fixture has: 747175 (Live), 747201 (Preview), 747099 (Final)
        // Final must be excluded
        assertThat(pks).containsExactlyInAnyOrder(747175, 747201);
        assertThat(pks).doesNotContain(747099);
    }

    @Test
    void fetchActiveGamePks_requestsCorrectDateParam() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(fixtureBody)
                .addHeader("Content-Type", "application/json"));

        client.fetchActiveGamePks("2026-07-06");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("date=2026-07-06");
        assertThat(request.getPath()).contains("sportId=1");
    }

    @Test
    void fetchActiveGamePks_returnsEmptyList_whenNoGames() {
        String emptyBody = "{\"totalItems\":0,\"dates\":[]}";
        server.enqueue(new MockResponse()
                .setBody(emptyBody)
                .addHeader("Content-Type", "application/json"));

        List<Integer> pks = client.fetchActiveGamePks("2026-07-04");

        assertThat(pks).isEmpty();
    }
}
