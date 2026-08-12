package com.ftxeven.airauctions.util;

import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class Version {

    private static final String VERSION_URL = "https://api.spiget.org/v2/resources/133357/versions/latest";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Plugin PLUGIN = JavaPlugin.getProvidingPlugin(Version.class);

    private static volatile String latestVersion;
    private static volatile boolean outdated;

    private Version() {
    }

    public static void check() {
        Scheduler.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(VERSION_URL))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                latestVersion = JsonParser.parseString(response.body()).getAsJsonObject().get("name").getAsString();

                String current = PLUGIN.getPluginMeta().getVersion();
                outdated = !current.equals(latestVersion);

                if (outdated) {
                    PLUGIN.getLogger().warning("Outdated! Running " + current + ", latest is " + latestVersion);
                } else {
                    PLUGIN.getLogger().info("Running the latest version (" + current + ")");
                }
            } catch (Exception e) {
                // spiget being flaky shouldn't flip outdated to true, just skip this check
                PLUGIN.getLogger().warning("Couldn't check for updates: " + e.getMessage());
            }
        });
    }

    public static String current() { return PLUGIN.getPluginMeta().getVersion(); }

    public static boolean isOutdated() {
        return outdated;
    }

    public static String getLatest() {
        return latestVersion;
    }
}