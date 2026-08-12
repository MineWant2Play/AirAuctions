package com.ftxeven.airauctions.command.admin;

import com.ftxeven.airauctions.AirAuctions;
import com.ftxeven.airauctions.command.SubCommand;
import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.permission.Permissions;
import com.ftxeven.airauctions.util.Messenger;
import com.ftxeven.airauctions.util.Version;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class SubVersion implements SubCommand {

    private final AirAuctions plugin;
    private final Messenger messenger;
    private final ConfigManager configs;

    public SubVersion(AirAuctions plugin, Messenger messenger, ConfigManager configs) {
        this.plugin = plugin;
        this.messenger = messenger;
        this.configs = configs;
    }

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String permission() {
        return Permissions.ADMIN;
    }

    @Override
    public String usage() {
        return "/airauctions version";
    }

    @Override
    public void execute(CommandSender sender, String label, String subLabel, String[] args) {
        String current = plugin.getPluginMeta().getVersion();
        messenger.send(sender, configs.lang().get("general.commands.version"),
                Map.of("version", current));

        if (Version.isOutdated()) {
            messenger.send(sender, configs.lang().get("general.commands.outdated"),
                    Map.of("current", current, "latest", Version.getLatest()));
        }
    }
}