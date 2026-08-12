package com.ftxeven.airauctions.command.admin;

import com.ftxeven.airauctions.AirAuctions;
import com.ftxeven.airauctions.command.SubCommand;
import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.permission.Permissions;
import com.ftxeven.airauctions.util.Messenger;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class SubReload implements SubCommand {

    private final AirAuctions plugin;
    private final Messenger messenger;
    private final ConfigManager configs;

    public SubReload(AirAuctions plugin, Messenger messenger, ConfigManager configs) {
        this.plugin = plugin;
        this.messenger = messenger;
        this.configs = configs;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String permission() {
        return Permissions.ADMIN;
    }

    @Override
    public String usage() {
        return "/airauctions reload";
    }

    @Override
    public void execute(CommandSender sender, String label, String subLabel, String[] args) {
        long start = System.currentTimeMillis();

        if (!configs.reload()) {
            messenger.send(sender, configs.lang().get("errors.reload-failed"));
            return;
        }

        plugin.economy().reload();
        plugin.hooks().reload();
        plugin.services().reload();

        if (!plugin.listingGuis().reload()) {
            messenger.send(sender, configs.lang().get("errors.reload-failed"));
            return;
        }

        long elapsed = System.currentTimeMillis() - start;
        messenger.send(sender, configs.lang().get("general.commands.reload"), Map.of("time", String.valueOf(elapsed)));
    }
}