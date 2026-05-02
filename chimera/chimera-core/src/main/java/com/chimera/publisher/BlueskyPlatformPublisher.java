package com.chimera.publisher;

import com.chimera.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Real PlatformPublisher that posts to Bluesky via the AT Protocol HTTP API.
 *
 * Flow per publish:
 *   1. POST /xrpc/com.atproto.server.createSession  -> obtain JWT
 *   2. POST /xrpc/com.atproto.repo.createRecord    -> create the post
 *
 * The publisher never throws -- failures (auth, network, rate limit) become
 * PublishResult.FAILED with an error string, so the pipeline can continue.
 */
public class BlueskyPlatformPublisher implements PlatformPublisher {

    private static final String SESSION_URL =
            "https://bsky.social/xrpc/com.atproto.server.createSession";
    private static final String CREATE_RECORD_URL =
            "https://bsky.social/xrpc/com.atproto.repo.createRecord";
    private static final int MAX_POST_LENGTH = 300;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http;
    private final String handle;
    private final String appPassword;

    public BlueskyPlatformPublisher(Config config) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.handle = config.blueskyHandle();
        this.appPassword = config.blueskyAppPassword();
    }

    @Override
    public PublishResult publish(PublishRequest request) {
        try {
            Session session = login();
            String postUri = createPost(session, truncate(request.caption()));
            return new PublishResult(
                    request.contentId(),
                    request.targetPlatform(),
                    PublishStatus.PUBLISHED,
                    Optional.of(postUri),
                    Optional.of(Instant.now()),
                    Optional.empty()
            );
        } catch (Exception e) {
            return new PublishResult(
                    request.contentId(),
                    request.targetPlatform(),
                    PublishStatus.FAILED,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(e.getClass().getSimpleName() + ": " + e.getMessage())
            );
        }
    }

    // --- HTTP calls ---

    private Session login() throws IOException, InterruptedException {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("identifier", handle);
        body.put("password", appPassword);

        JsonNode response = postJson(SESSION_URL, body, /* bearer */ null);

        return new Session(
                requireText(response, "accessJwt"),
                requireText(response, "did")
        );
    }

    private String createPost(Session session, String text) throws IOException, InterruptedException {
        ObjectNode record = MAPPER.createObjectNode();
        record.put("$type", "app.bsky.feed.post");
        record.put("text", text);
        record.put("createdAt", Instant.now().toString());

        ObjectNode body = MAPPER.createObjectNode();
        body.put("repo", session.did);
        body.put("collection", "app.bsky.feed.post");
        body.set("record", record);

        JsonNode response = postJson(CREATE_RECORD_URL, body, session.accessJwt);
        return requireText(response, "uri");
    }

    private JsonNode postJson(String url, ObjectNode body, String bearer) throws IOException, InterruptedException {
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));

        if (bearer != null) {
            rb.header("Authorization", "Bearer " + bearer);
        }

        HttpResponse<String> response = http.send(rb.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Bluesky returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return MAPPER.readTree(response.body());
    }

    // --- helpers ---

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= MAX_POST_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_POST_LENGTH - 1) + "…";  // unicode ellipsis
    }

    private static String requireText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || !v.isTextual()) {
            throw new IllegalStateException(
                    "Expected '" + field + "' (text) in Bluesky response: " + node);
        }
        return v.asText();
    }

    private record Session(String accessJwt, String did) {
    }
}
