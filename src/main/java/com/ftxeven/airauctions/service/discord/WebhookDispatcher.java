package com.ftxeven.airauctions.service.discord;

import com.ftxeven.airauctions.util.Scheduler;
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
        Scheduler.runAsync(() -> post(webhookUrl, payload));
    }

    private void post(String webhookUrl, JsonObject payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                logger.warning("Discord webhook returned HTTP " + response.statusCode() + ": " + response.body());
            }
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid Discord webhook URL, skipping: " + webhookUrl);
        } catch (Exception e) {
            logger.warning("Could not deliver Discord webhook: " + e.getMessage());
        }
    }
}