package com.ftxeven.airauctions.service.discord;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

final class WebhookDispatcher {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final Logger logger;

    WebhookDispatcher(Logger logger) {
        this.logger = logger;
    }

    void send(String webhookUrl, JsonObject payload) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid Discord webhook URL, skipping: " + webhookUrl);
            return;
        }

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 300) {
                        logger.warning("Discord webhook returned HTTP " + response.statusCode() + ": " + response.body());
                    }
                })
                .exceptionally(e -> {
                    logger.warning("Could not deliver Discord webhook: " + e.getMessage());
                    return null;
                });
    }
}