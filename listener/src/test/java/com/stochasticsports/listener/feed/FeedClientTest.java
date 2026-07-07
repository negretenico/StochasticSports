package com.stochasticsports.listener.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FeedClientTest {

    private MockWebServer server;
    private FeedClient feedClient;
    private String fixtureBody;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        feedClient = new FeedClient(webClient);

        try (InputStream is = getClass().getResourceAsStream("/fixtures/feed_live_response.json")) {
            fixtureBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    // ── Cycle 7: FeedClient.fetch(gamePk, null) — no timecode param ──────────

    @Test
    void fetch_withNullTimecode_doesNotIncludeTimecodeParam() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(fixtureBody)
                .addHeader("Content-Type", "application/json"));

        feedClient.fetch(747175, null);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).doesNotContain("timecode");
        assertThat(request.getPath()).contains("/game/747175/feed/live");
    }

    // ── Cycle 8: FeedClient.fetch(gamePk, timecode) — includes timecode ───────

    @Test
    void fetch_withTimecode_includesTimecodeQueryParam() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(fixtureBody)
                .addHeader("Content-Type", "application/json"));

        feedClient.fetch(747175, "20260704_150000");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).contains("/game/747175/feed/live");
        assertThat(request.getPath()).contains("timecode=20260704_150000");
    }

    @Test
    void fetch_returnsDeserializedFeed() {
        server.enqueue(new MockResponse()
                .setBody(fixtureBody)
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> feed = feedClient.fetch(747175, null);

        assertThat(feed).containsKey("gameData");
        assertThat(feed).containsKey("liveData");
        assertThat(feed).containsKey("metaData");
    }
}
