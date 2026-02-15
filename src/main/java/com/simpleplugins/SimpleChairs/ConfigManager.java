package com.simpleplugins.SimpleChairs;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigManager {

    private final SimpleChairs plugin;
    private Set<String> enabledSitBlockGroups;
    private Set<Material> materialBlacklist;

    public ConfigManager(SimpleChairs plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        loadSitBlockGroups();
        loadMaterialBlacklist();
    }

    private void loadSitBlockGroups() {
        List<String> raw = getConfig().getStringList("sit-blocks");
        if (raw == null) {
            enabledSitBlockGroups = Set.of("stairs");
            return;
        }
        enabledSitBlockGroups = raw.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(s -> !s.startsWith("#"))
                .map(s -> s.toLowerCase())
                .collect(Collectors.toSet());
    }

    private void loadMaterialBlacklist() {
        List<String> raw = getConfig().getStringList("material-blacklist");
        if (raw == null) {
            materialBlacklist = Set.of();
            return;
        }
        materialBlacklist = raw.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .map(s -> {
                    try {
                        return Material.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(m -> m != null)
                .collect(Collectors.toSet());
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public String getPrefix() {
        return getConfig().getString("prefix", "&7[&6SimpleChairs&7] &r");
    }

    public boolean isWorldGuardIntegration() {
        return getConfig().getBoolean("worldguard-integration", true);
    }

    public double getMaxDistance() {
        return getConfig().getDouble("sit-max-distance", 0);
    }

    public int getMaxBlockHeight() {
        return getConfig().getInt("sit-max-block-height", 2);
    }

    public boolean isPersistSitHint() {
        return getConfig().getBoolean("persist-sit-hint", false);
    }

    public boolean isPersistCrawlHint() {
        return getConfig().getBoolean("persist-crawl-hint", false);
    }

    public boolean isOneBlockPerChair() {
        return getConfig().getBoolean("one-block-per-chair", true);
    }

    public boolean isAllowUnsafe() {
        return getConfig().getBoolean("allow-unsafe-sit", false);
    }

    public boolean isBottomPartOnly() {
        return getConfig().getBoolean("sit-bottom-part-only", true);
    }

    public boolean isEmptyHandOnly() {
        return getConfig().getBoolean("sit-empty-hand-only", true);
    }

    public boolean isCenterBlock() {
        return getConfig().getBoolean("sit-center-on-block", true);
    }

    public boolean isStandWhenDamaged() {
        return getConfig().getBoolean("stand-when-damaged", false);
    }

    public boolean isSneakToStand() {
        return getConfig().getBoolean("sneak-to-stand", true);
    }

    public boolean isReturnOnStand() {
        return getConfig().getBoolean("return-on-stand", false);
    }

    public boolean isStandWhenBlockBreaks() {
        return getConfig().getBoolean("stand-when-block-breaks", true);
    }

    public boolean isCrawlEnabled() {
        return getConfig().getBoolean("features.crawl-enabled", true);
    }

    public boolean isCrawlSneakToStand() {
        return getConfig().getBoolean("crawl-sneak-to-stand", true);
    }

    public boolean isCrawlDoubleSneak() {
        return getConfig().getBoolean("crawl-double-sneak", false);
    }

    public boolean isReturnToOriginalPosition() {
        return isReturnOnStand();
    }

    public boolean isWorldFilterEnabled() {
        return getConfig().getBoolean("world-filter-enabled", false);
    }

    public List<String> getWorlds() {
        List<String> w = getConfig().getStringList("worlds");
        return w != null ? w : Collections.emptyList();
    }

    public boolean isMaterialBlacklisted(Material material) {
        return materialBlacklist != null && materialBlacklist.contains(material);
    }

    public Set<String> getEnabledSitBlockGroups() {
        return enabledSitBlockGroups == null ? Set.of("stairs") : Collections.unmodifiableSet(enabledSitBlockGroups);
    }

    public String getPluginMessage(String key) {
        return getConfig().getString("plugin-messages." + key, "");
    }

    public String getPlayerMessage(String key) {
        return getConfig().getString("player-messages." + key, "");
    }
}
