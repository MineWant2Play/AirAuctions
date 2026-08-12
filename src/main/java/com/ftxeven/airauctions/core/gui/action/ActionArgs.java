package com.ftxeven.airauctions.core.gui.action;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ActionArgs {

    private ActionArgs() {
    }

    public static Map<String, String> parse(String raw) {
        Map<String, String> args = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return args;
        }
        for (String token : raw.trim().split("\\s+")) {
            int sep = token.indexOf(':');
            if (sep > 0) {
                args.put(token.substring(0, sep), token.substring(sep + 1));
            }
        }
        return args;
    }
}