package com.simpleplugins.SimpleChairs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CrawlCommand implements CommandExecutor {

    private final SimpleChairs plugin;
    private final ChairListener chairListener;

    public CrawlCommand(SimpleChairs plugin, ChairListener chairListener) {
        this.plugin = plugin;
        this.chairListener = chairListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("player-only"));
            return true;
        }
        if (!plugin.getConfigManager().isCrawlEnabled()) {
            player.sendMessage(plugin.getMessage("feature-disabled"));
            return true;
        }
        if (!player.hasPermission("schairs.crawl")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }
        chairListener.toggleCrawl(player);
        return true;
    }
}
