package com.ftxeven.airauctions.service.discord;

import com.ftxeven.airauctions.config.ExpansionsConfig;
import com.ftxeven.airauctions.util.Messenger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.List;
import java.util.Map;

final class EmbedRenderer {

    private final Messenger messenger;

    EmbedRenderer(Messenger messenger) {
        this.messenger = messenger;
    }

    JsonObject payload(ExpansionsConfig.DiscordEvent event, Map<String, String> placeholders) {
        JsonObject embed = new JsonObject();

        putIfPresent(embed, "title", resolve(event.title(), placeholders));
        putIfPresent(embed, "url", resolve(event.titleUrl(), placeholders));
        putIfPresent(embed, "description", resolve(event.description(), placeholders));
        if (event.color() != 0) {
            embed.addProperty("color", event.color());
        }

        JsonObject author = author(event.author(), placeholders);
        if (author != null) {
            embed.add("author", author);
        }

        JsonObject thumbnail = urlObject(resolve(event.thumbnailUrl(), placeholders));
        if (thumbnail != null) {
            embed.add("thumbnail", thumbnail);
        }

        JsonObject image = urlObject(resolve(event.imageUrl(), placeholders));
        if (image != null) {
            embed.add("image", image);
        }

        JsonArray fields = fields(event.fields(), placeholders);
        if (!fields.isEmpty()) {
            embed.add("fields", fields);
        }

        JsonObject footer = footer(event.footer(), placeholders);
        if (footer != null) {
            embed.add("footer", footer);
        }

        if (event.timestamp()) {
            embed.addProperty("timestamp", Instant.now().toString());
        }

        JsonArray embeds = new JsonArray();
        embeds.add(embed);

        JsonObject body = new JsonObject();
        body.add("embeds", embeds);
        return body;
    }

    private JsonObject author(ExpansionsConfig.Author author, Map<String, String> placeholders) {
        String name = resolve(author.name(), placeholders);
        if (name == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        putIfPresent(json, "icon_url", resolve(author.iconUrl(), placeholders));
        putIfPresent(json, "url", resolve(author.url(), placeholders));
        return json;
    }

    private JsonObject footer(ExpansionsConfig.Footer footer, Map<String, String> placeholders) {
        String text = resolve(footer.text(), placeholders);
        if (text == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        putIfPresent(json, "icon_url", resolve(footer.iconUrl(), placeholders));
        return json;
    }

    private JsonArray fields(List<ExpansionsConfig.EmbedField> configured, Map<String, String> placeholders) {
        JsonArray fields = new JsonArray();
        for (ExpansionsConfig.EmbedField field : configured) {
            String value = resolve(field.value(), placeholders);
            if (value == null) {
                continue;
            }
            String name = resolve(field.name(), placeholders);
            JsonObject json = new JsonObject();
            json.addProperty("name", name != null ? name : "\u200B");
            json.addProperty("value", value);
            json.addProperty("inline", field.inline());
            fields.add(json);
        }
        return fields;
    }

    private static JsonObject urlObject(String url) {
        if (url == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("url", url);
        return json;
    }

    private static void putIfPresent(JsonObject json, String key, String value) {
        if (value != null) {
            json.addProperty(key, value);
        }
    }

    private String resolve(String raw, Map<String, String> placeholders) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String rendered = messenger.plain(raw, placeholders);
        return rendered.isEmpty() ? null : rendered;
    }
}