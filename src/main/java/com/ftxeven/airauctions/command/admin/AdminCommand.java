package com.ftxeven.airauctions.command.admin;

import com.ftxeven.airauctions.AirAuctions;
import com.ftxeven.airauctions.command.CommandDispatcher;
import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.core.command.CommandRegistry;
import com.ftxeven.airauctions.util.Messenger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public final class AdminCommand implements CommandExecutor, TabCompleter {

    private final Messenger messenger;
    private final ConfigManager configs;
    private final CommandRegistry registry;
    private final CommandDispatcher dispatcher;

    public AdminCommand(AirAuctions plugin) {
        this.messenger = plugin.messenger();
        this.configs = plugin.configs();
        this.registry = new CommandRegistry()
                .register(new SubReload(plugin, messenger, configs))
                .register(new SubVersion(plugin, messenger, configs))
                .register(new SubSimulate(messenger, configs, plugin.services(), plugin.getLogger()));
        this.dispatcher = new CommandDispatcher(messenger, configs);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        dispatcher.dispatch(registry, sender, label, args, () -> sendUsage(sender));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return dispatcher.tabComplete(registry, sender, args);
    }

    private void sendUsage(CommandSender sender) {
        messenger.send(sender, configs.lang().get("general.commands.usage"));
    }
}