package com.ftxeven.airauctions.core.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class DynamicCommandRegistry {

    private final JavaPlugin plugin;
    private final CommandMap commandMap;
    private final Set<Command> registered = Collections.newSetFromMap(new IdentityHashMap<>());

    public DynamicCommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.commandMap = resolveCommandMap();
        removeStaleRegistrations();
    }

    public <T extends CommandExecutor & TabCompleter> void register(String name, List<String> aliases, T handler) {
        register(name, aliases, handler, UnaryOperator.identity());
    }

    public <T extends CommandExecutor & TabCompleter> void register(String name, List<String> aliases, T handler, UnaryOperator<String[]> argsTransform) {
        if (commandMap == null) {
            plugin.getLogger().severe("Could not register command '" + name + "' - the server's command map is unavailable");
            return;
        }

        Command command = new RegisteredCommand(plugin, name, handler, handler, argsTransform);
        command.setAliases(aliases);
        commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), command);
        registered.add(command);
    }

    public void unregisterAll() {
        if (commandMap == null || registered.isEmpty()) {
            return;
        }

        removeCommands(registered);
        registered.clear();
    }

    private CommandMap resolveCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().severe("Could not access the server's command map: " + e.getMessage());
            return null;
        }
    }

    private void removeStaleRegistrations() {
        if (commandMap == null) {
            return;
        }

        Set<Command> stale = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Command command : commandMap.getKnownCommands().values()) {
            String className = command.getClass().getName();
            if (className.equals(DynamicCommandRegistry.class.getName() + "$1")
                    || className.equals(RegisteredCommand.class.getName())) {
                stale.add(command);
            }
        }

        if (!stale.isEmpty()) {
            removeCommands(stale);
            plugin.getLogger().warning("Removed " + stale.size()
                    + " stale dynamic command(s) left by an earlier AirAuctions instance");
        }
    }

    private void removeCommands(Set<Command> commands) {
        Map<String, Command> knownCommands = commandMap.getKnownCommands();
        knownCommands.entrySet().removeIf(entry -> commands.contains(entry.getValue()));
        for (Command command : commands) {
            command.unregister(commandMap);
        }
    }

    private static final class RegisteredCommand extends Command implements PluginIdentifiableCommand {

        private final JavaPlugin plugin;
        private final CommandExecutor executor;
        private final TabCompleter tabCompleter;
        private final UnaryOperator<String[]> argsTransform;

        private RegisteredCommand(JavaPlugin plugin, String name, CommandExecutor executor,
                                  TabCompleter tabCompleter, UnaryOperator<String[]> argsTransform) {
            super(name);
            this.plugin = plugin;
            this.executor = executor;
            this.tabCompleter = tabCompleter;
            this.argsTransform = argsTransform;
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return executor.onCommand(sender, this, label, argsTransform.apply(args));
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String label, String[] args) {
            List<String> completions = tabCompleter.onTabComplete(sender, this, label, argsTransform.apply(args));
            return completions != null ? completions : List.of();
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }
    }
}
