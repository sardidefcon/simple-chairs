package com.simpleplugins.SimpleChairs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleChairsCommand implements CommandExecutor, TabCompleter {

    private final SimpleChairs plugin;

    public SimpleChairsCommand(SimpleChairs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("schairs.reload")) {
                sender.sendMessage(plugin.getMessage("reload-no-permission"));
                return true;
            }
            plugin.reloadPluginConfig();
            sender.sendMessage(plugin.getMessage("reload-success"));
            return true;
        }
        sendUsage(sender, label);
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        String usage = plugin.getConfigManager().getPluginMessage("usage");
        if (usage != null && !usage.isEmpty()) {
            usage = usage.replace("<command>", label);
            String prefix = plugin.getConfigManager().getPrefix();
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + usage));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("schairs.reload")) {
            return Stream.of("reload")
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
