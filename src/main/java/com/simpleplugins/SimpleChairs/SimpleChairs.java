package com.simpleplugins.SimpleChairs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SimpleChairs extends JavaPlugin {

    private ConfigManager configManager;
    private NamespacedKey chairStandKey;
    private WorldGuardIntegration worldGuardIntegration;
    private ChairListener chairListener;
    private BukkitTask sitHintTask;
    private BukkitTask crawlTickTask;
    private BukkitTask chairRotationTickTask;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigUpdater.mergeWithDefaults(this);
        this.configManager = new ConfigManager(this);
        this.chairStandKey = new NamespacedKey(this, "simplechairs");

        cleanupGhostChairStands();

        if (configManager.isWorldGuardIntegration() && getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                this.worldGuardIntegration = new WorldGuardIntegration(this);
                getLogger().info("WorldGuard integration enabled (chairs-deny flag).");
            } catch (Throwable t) {
                getLogger().warning("WorldGuard integration could not be enabled: " + t.getMessage());
                this.worldGuardIntegration = null;
            }
        } else {
            this.worldGuardIntegration = null;
        }

        this.chairListener = new ChairListener(this);
        getServer().getPluginManager().registerEvents(chairListener, this);

        SimpleChairsCommand cmd = new SimpleChairsCommand(this);
        getCommand("simplechairs").setExecutor(cmd);
        getCommand("simplechairs").setTabCompleter(cmd);

        getCommand("sit").setExecutor(new SitCommand(this, chairListener));
        getCommand("crawl").setExecutor(new CrawlCommand(this, chairListener));

        startSitHintTask();
        startChairRotationTickTask();
        if (configManager.isCrawlEnabled()) startCrawlTickTask();

        getLogger().info("SimpleChairs has been enabled.");
    }

    @Override
    public void onDisable() {
        stopSitHintTask();
        stopChairRotationTickTask();
        stopCrawlTickTask();
        ChairListener.cleanupAllChairs(this);
        getLogger().info("SimpleChairs has been disabled.");
    }

    private void startSitHintTask() {
        stopSitHintTask();
        if (!configManager.isPersistSitHint() && !configManager.isPersistCrawlHint()) {
            return;
        }
        final boolean persistSit = configManager.isPersistSitHint();
        final boolean persistCrawl = configManager.isPersistCrawlHint();
        sitHintTask = getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                if (persistSit && player.isInsideVehicle() && isChairStand(player.getVehicle())) {
                    sendPlayerActionBar(player, "sit-hint");
                } else if (persistCrawl && ChairListener.isCrawling(player.getUniqueId())) {
                    sendPlayerActionBar(player, "sit-hint");
                }
            }
        }, 20L, 20L);
    }

    private void stopSitHintTask() {
        if (sitHintTask != null) {
            sitHintTask.cancel();
            sitHintTask = null;
        }
    }

    private void startChairRotationTickTask() {
        stopChairRotationTickTask();
        chairRotationTickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (chairListener != null) chairListener.tickChairRotation();
        }, 1L, 1L);
    }

    private void stopChairRotationTickTask() {
        if (chairRotationTickTask != null) {
            chairRotationTickTask.cancel();
            chairRotationTickTask = null;
        }
    }

    private void startCrawlTickTask() {
        stopCrawlTickTask();
        crawlTickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            if (chairListener != null) chairListener.tickCrawl();
        }, 1L, 1L);
    }

    private void stopCrawlTickTask() {
        if (crawlTickTask != null) {
            crawlTickTask.cancel();
            crawlTickTask = null;
        }
    }

    /**
     * Removes any armor stands marked as SimpleChairs (e.g. left after a crash).
     */
    private void cleanupGhostChairStands() {
        getServer().getWorlds().forEach(world ->
                world.getEntitiesByClass(ArmorStand.class).stream()
                        .filter(this::isChairStand)
                        .forEach(stand -> {
                            stand.eject();
                            stand.remove();
                        }));
    }

    /**
     * Merges defaults with existing config (adding only missing keys), reloads
     * from disk, and refreshes config-dependent state. Existing user values are
     * never overwritten. On failure, config and state are left unchanged.
     */
    public void reloadPluginConfig() {
        ConfigUpdater.mergeWithDefaults(this);
        reloadConfig();
        configManager.reload();
        stopCrawlTickTask();
        if (configManager.isCrawlEnabled()) startCrawlTickTask();
        if (configManager.isWorldGuardIntegration() && getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                this.worldGuardIntegration = new WorldGuardIntegration(this);
            } catch (Throwable t) {
                this.worldGuardIntegration = null;
            }
        } else {
            this.worldGuardIntegration = null;
        }
        startSitHintTask();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public NamespacedKey getChairStandKey() {
        return chairStandKey;
    }

    /**
     * Returns true if the entity is an armor stand marked as a SimpleChairs seat (by PDC).
     */
    public boolean isChairStand(Entity entity) {
        return entity instanceof ArmorStand
                && entity.getPersistentDataContainer().has(chairStandKey, PersistentDataType.INTEGER);
    }

    public WorldGuardIntegration getWorldGuardIntegration() {
        return worldGuardIntegration;
    }

    /**
     * Gets a plugin message (for chat) with prefix and color codes.
     */
    public String getMessage(String key) {
        String prefix = configManager.getPrefix();
        String msg = configManager.getPluginMessage(key);
        if (msg == null || msg.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    /**
     * Sends a player message above the experience bar (action bar), no prefix.
     */
    public void sendPlayerActionBar(Player player, String key) {
        String msg = configManager.getPlayerMessage(key);
        if (msg == null || msg.isEmpty()) {
            return;
        }
        String colored = ChatColor.translateAlternateColorCodes('&', msg);
        Component component = LEGACY.deserialize(colored);
        player.sendActionBar(component);
    }
}
