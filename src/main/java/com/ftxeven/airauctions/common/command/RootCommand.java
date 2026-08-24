package com.ftxeven.airauctions.common.command;

import java.util.List;

public record RootCommand(String name, List<String> aliases, String usage) {
    public RootCommand {
        aliases = List.copyOf(aliases);
    }
}