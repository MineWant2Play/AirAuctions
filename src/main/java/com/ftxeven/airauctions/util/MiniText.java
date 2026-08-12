package com.ftxeven.airauctions.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class MiniText {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MiniText() {
    }

    public static MiniMessage mini() {
        return MINI;
    }

    public static Component parse(String miniMessageText) {
        return MINI.deserialize(miniMessageText);
    }

    public static String plain(Component component) {
        return component != null ? PlainTextComponentSerializer.plainText().serialize(component) : "";
    }

    public static String plain(String miniMessageText) {
        return plain(parse(miniMessageText));
    }
}