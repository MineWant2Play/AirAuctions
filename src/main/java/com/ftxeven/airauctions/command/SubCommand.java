package com.ftxeven.airauctions.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    String name();

    default List<String> aliases() {
        return List.of();
    }

    default boolean enabled() {
        return true;
    }

    default String permission() {
        return null;
    }

    default boolean playerOnly() {
        return false;
    }

    default int minArgs() {
        return 0;
    }

    default int minArgs(CommandSender sender) {
        return minArgs();
    }

    default int maxArgs() {
        return 0;
    }

    default int maxArgs(CommandSender sender) {
        return maxArgs();
    }

    String usage();

    default String usage(CommandSender sender) {
        return usage();
    }

    void execute(CommandSender sender, String label, String subLabel, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}