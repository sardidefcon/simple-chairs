package com.simpleplugins.SimpleChairs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChairListener implements Listener {

    private final SimpleChairs plugin;
    private static final Set<UUID> chairStands = new HashSet<>();
    private static final Map<UUID, Location> standToBlock = new HashMap<>();
    private static final Map<BlockKey, UUID> blockToStand = new HashMap<>();
    private static final Map<UUID, Location> playerToPreviousLocation = new HashMap<>();
    private static final Set<UUID> crawlingPlayers = new HashSet<>();
    private static final Map<UUID, Location> crawlBarrierAt = new HashMap<>();
    private static final Map<UUID, Long> doubleSneakCrawlTime = new HashMap<>();

    public ChairListener(SimpleChairs plugin) {
        this.plugin = plugin;
    }

    private static final class BlockKey {
        final String key;
        BlockKey(Block block) {
            this.key = block.getWorld().getUID() + " " + block.getX() + " " + block.getY() + " " + block.getZ();
        }
        BlockKey(Location blockLoc) {
            this.key = blockLoc.getWorld().getUID() + " " + blockLoc.getBlockX() + " " + blockLoc.getBlockY() + " " + blockLoc.getBlockZ();
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof BlockKey && key.equals(((BlockKey) o).key);
        }
        @Override
        public int hashCode() {
            return key.hashCode();
        }
    }

    private boolean isBlockInEnabledGroup(Block block, Set<String> groups) {
        if (Tag.STAIRS.isTagged(block.getType()) && groups.contains("stairs")) {
            return true;
        }
        if (Tag.SLABS.isTagged(block.getType()) && groups.contains("slabs")) {
            if (block.getBlockData() instanceof Slab slab && slab.getType() == Slab.Type.BOTTOM) {
                return true;
            }
        }
        if (groups.contains("carpets") && block.getType().name().endsWith("_CARPET")) {
            return true;
        }
        return false;
    }

    private boolean isSafePosition(Block block) {
        if (plugin.getConfigManager().isAllowUnsafe()) {
            return true;
        }
        if (block.getType().name().contains("LAVA") || block.getType().name().contains("FIRE")) {
            return false;
        }
        org.bukkit.block.Block above = block.getRelative(0, 1, 0);
        return !above.getType().isSolid();
    }

    /** Base offset for seat height (GSit-style). 1.20.2+ uses -0.05 so the passenger renders on the surface. */
    private static final double BASE_OFFSET = -0.05;
    /** For stairs/slabs: seat is this much below the step top so the player sits on the surface (GSit: 0.5 - 0.05 = 0.45). */
    private static final double STAIR_SIT_BELOW_TOP = 0.45;
    /** Small XZ offset for stairs so the player is centered on the step (GSit STAIR_XZ_OFFSET). */
    private static final double STAIR_XZ_OFFSET = 0.123;

    /**
     * Computes the seat location so the player appears seated on the block (GSit-style).
     * Uses the block's bounding box and a base offset so the passenger is not floating.
     */
    private Location getSeatLocation(Block block, float playerYaw) {
        Location blockLoc = block.getLocation();
        double bx = blockLoc.getX();
        double by = blockLoc.getY();
        double bz = blockLoc.getZ();
        double centerX = 0.5;
        double centerZ = 0.5;
        float yaw = playerYaw;

        double blockHeight = block.getBoundingBox().getMaxY() - block.getBoundingBox().getMinY();
        boolean isStair = Tag.STAIRS.isTagged(block.getType());
        boolean isSlab = Tag.SLABS.isTagged(block.getType()) && block.getBlockData() instanceof Slab s && s.getType() == Slab.Type.BOTTOM;

        double seatY;
        if (isStair) {
            seatY = by + blockHeight - STAIR_SIT_BELOW_TOP;
            if (block.getBlockData() instanceof Stairs stairs && stairs.getHalf() == Bisected.Half.BOTTOM) {
                BlockFace face = stairs.getFacing().getOppositeFace();
                double xo = 0, zo = 0;
                switch (face) {
                    case EAST -> { xo = STAIR_XZ_OFFSET; zo = 0; yaw = -90f; }
                    case SOUTH -> { xo = 0; zo = STAIR_XZ_OFFSET; yaw = 0f; }
                    case WEST -> { xo = -STAIR_XZ_OFFSET; zo = 0; yaw = 90f; }
                    case NORTH -> { xo = 0; zo = -STAIR_XZ_OFFSET; yaw = 180f; }
                    default -> { }
                }
                centerX = 0.5 + xo;
                centerZ = 0.5 + zo;
            }
        } else if (isSlab) {
            seatY = by + blockHeight + BASE_OFFSET;
        } else {
            seatY = by + blockHeight + (blockHeight >= 0.5 ? BASE_OFFSET : 0);
        }

        Location loc = new Location(block.getWorld(), bx + centerX, seatY, bz + centerZ);
        loc.setYaw(yaw);
        loc.setPitch(0f);
        return loc;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getBlockFace() != BlockFace.UP) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        ConfigManager config = plugin.getConfigManager();
        Set<String> enabledGroups = config.getEnabledSitBlockGroups();

        if (!player.hasPermission("schairs.sit")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        if (config.isEmptyHandOnly() && event.getItem() != null && !event.getItem().getType().isAir()) {
            return;
        }

        if (player.isSneaking()) {
            return;
        }

        if (player.isInsideVehicle()) {
            plugin.sendPlayerActionBar(player, "already-sitting");
            return;
        }

        if (crawlingPlayers.contains(player.getUniqueId())) {
            return;
        }

        if (!isBlockInEnabledGroup(block, enabledGroups)) {
            return;
        }

        if (config.isBottomPartOnly() && block.getBlockData() instanceof Stairs stairs && stairs.getHalf() == Bisected.Half.TOP) {
            return;
        }

        if (config.isMaterialBlacklisted(block.getType())) {
            return;
        }

        int maxBlockHeight = config.getMaxBlockHeight();
        if (block.getY() > player.getLocation().getBlockY() + maxBlockHeight) {
            return;
        }

        if (config.isWorldFilterEnabled() && !config.getWorlds().contains(block.getWorld().getName())) {
            player.sendMessage(plugin.getMessage("world-not-allowed"));
            return;
        }

        if (plugin.getWorldGuardIntegration() != null && plugin.getWorldGuardIntegration().isChairDenied(block.getLocation())) {
            return;
        }

        double maxDistance = config.getMaxDistance();
        if (maxDistance > 0 && player.getLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5)) > maxDistance * maxDistance) {
            return;
        }

        BlockKey key = new BlockKey(block);
        if (config.isOneBlockPerChair() && blockToStand.containsKey(key)) {
            plugin.sendPlayerActionBar(player, "chair-occupied");
            return;
        }

        if (!isSafePosition(block)) {
            return;
        }

        Location standLoc = getSeatLocation(block, player.getLocation().getYaw());

        ArmorStand stand = block.getWorld().spawn(standLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setSmall(true);
            as.setMarker(true);
            as.setSilent(true);
            as.getPersistentDataContainer().set(plugin.getChairStandKey(), PersistentDataType.INTEGER, 1);
        });

        chairStands.add(stand.getUniqueId());
        standToBlock.put(stand.getUniqueId(), block.getLocation().getBlock().getLocation());
        blockToStand.put(key, stand.getUniqueId());

        if (config.isReturnOnStand()) {
            playerToPreviousLocation.put(player.getUniqueId(), player.getLocation().clone());
        }

        // GSit-style: add passenger next tick so the seat entity is fully spawned (better compatibility).
        Player p = player;
        org.bukkit.entity.ArmorStand s = stand;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (s.isValid() && p.isOnline() && !p.isInsideVehicle()) {
                s.addPassenger(p);
            }
            plugin.sendPlayerActionBar(p, "sit-hint");
        });
    }

    public boolean sitOnGround(Player player) {
        ConfigManager config = plugin.getConfigManager();
        if (player.isInsideVehicle()) {
            plugin.sendPlayerActionBar(player, "already-sitting");
            return false;
        }
        if (config.isWorldFilterEnabled() && !config.getWorlds().contains(player.getWorld().getName())) {
            player.sendMessage(plugin.getMessage("world-not-allowed"));
            return false;
        }
        org.bukkit.block.Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
        if (!block.getType().isSolid() || !isSafePosition(block)) {
            return false;
        }
        Location standLoc = getSeatLocation(block, player.getLocation().getYaw());
        ArmorStand stand = block.getWorld().spawn(standLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setSmall(true);
            as.setMarker(true);
            as.setSilent(true);
            as.getPersistentDataContainer().set(plugin.getChairStandKey(), PersistentDataType.INTEGER, 1);
        });
        chairStands.add(stand.getUniqueId());
        standToBlock.put(stand.getUniqueId(), block.getLocation().getBlock().getLocation());
        if (config.isReturnOnStand()) {
            playerToPreviousLocation.put(player.getUniqueId(), player.getLocation().clone());
        }
        // GSit-style: add passenger next tick for better compatibility.
        Player p = player;
        org.bukkit.entity.ArmorStand s = stand;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (s.isValid() && p.isOnline() && !p.isInsideVehicle()) {
                s.addPassenger(p);
            }
            plugin.sendPlayerActionBar(p, "sit-hint");
        });
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!plugin.getConfigManager().isStandWhenDamaged()) return;
        if (!(event.getEntity() instanceof Player player) || event.getDamage() <= 0) return;
        if (player.isInsideVehicle()) {
            org.bukkit.entity.Entity vehicle = player.getVehicle();
            if (vehicle != null && chairStands.contains(vehicle.getUniqueId())) {
                vehicle.eject();
            }
        }
        if (crawlingPlayers.remove(player.getUniqueId())) {
            restoreCrawlBarrier(player.getUniqueId());
            player.setSwimming(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isInsideVehicle()) {
            return;
        }
        org.bukkit.entity.Entity vehicle = player.getVehicle();
        if (vehicle != null && plugin.isChairStand(vehicle)) {
            Location blockLoc = standToBlock.remove(vehicle.getUniqueId());
            if (blockLoc != null) blockToStand.remove(new BlockKey(blockLoc));
            chairStands.remove(vehicle.getUniqueId());
            vehicle.eject();
            vehicle.remove();
        }
    }

    /**
     * Prevents the game from turning off swimming while we want the player to stay crawling (GSit-style).
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityToggleSwim(EntityToggleSwimEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!crawlingPlayers.contains(player.getUniqueId())) {
            return;
        }
        if (event.isSwimming()) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (event.isSneaking()) {
            if (player.isInsideVehicle()) {
                org.bukkit.entity.Entity vehicle = player.getVehicle();
                if (vehicle != null && chairStands.contains(vehicle.getUniqueId())) {
                    if (plugin.getConfigManager().isSneakToStand()) {
                        vehicle.eject();
                    }
                }
                return;
            }
            if (plugin.getConfigManager().isCrawlSneakToStand() && crawlingPlayers.remove(player.getUniqueId())) {
                restoreCrawlBarrier(player.getUniqueId());
                player.setSwimming(false);
                return;
            }
            if (plugin.getConfigManager().isCrawlDoubleSneak() && !crawlingPlayers.contains(player.getUniqueId())
                    && plugin.getConfigManager().isCrawlEnabled()
                    && player.isOnGround() && player.getVehicle() == null && player.getLocation().getPitch() >= 85f
                    && player.hasPermission("schairs.crawl")) {
                Long last = doubleSneakCrawlTime.get(player.getUniqueId());
                long now = System.currentTimeMillis();
                doubleSneakCrawlTime.put(player.getUniqueId(), now);
                if (last != null && now - last <= 400L) {
                    doubleSneakCrawlTime.remove(player.getUniqueId());
                    if (plugin.getConfigManager().isWorldFilterEnabled() && !plugin.getConfigManager().getWorlds().contains(player.getWorld().getName())) {
                        return;
                    }
                    startCrawl(player);
                }
                return;
            }
        } else {
            doubleSneakCrawlTime.remove(player.getUniqueId());
        }
    }

    private boolean startCrawl(Player player) {
        if (crawlingPlayers.contains(player.getUniqueId())) return false;
        ConfigManager config = plugin.getConfigManager();
        if (config.isWorldFilterEnabled() && !config.getWorlds().contains(player.getWorld().getName())) {
            return false;
        }
        crawlingPlayers.add(player.getUniqueId());
        player.setSwimming(true);
        plugin.sendPlayerActionBar(player, "sit-hint");
        return true;
    }

    public boolean toggleCrawl(Player player) {
        if (crawlingPlayers.contains(player.getUniqueId())) {
            crawlingPlayers.remove(player.getUniqueId());
            restoreCrawlBarrier(player.getUniqueId());
            player.setSwimming(false);
            return false;
        }
        if (plugin.getConfigManager().isWorldFilterEnabled() && !plugin.getConfigManager().getWorlds().contains(player.getWorld().getName())) {
            player.sendMessage(plugin.getMessage("world-not-allowed"));
            return false;
        }
        return startCrawl(player);
    }

    private void restoreCrawlBarrier(UUID playerId) {
        Location at = crawlBarrierAt.remove(playerId);
        if (at == null) return;
        Player p = plugin.getServer().getPlayer(playerId);
        if (p != null && p.isOnline()) {
            p.sendBlockChange(at, at.getBlock().getBlockData());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMoveCrawl(PlayerMoveEvent event) {
        if (!crawlingPlayers.contains(event.getPlayer().getUniqueId())) return;
        if (event.getTo() == null) return;
        updateCrawlBarrierFor(event.getPlayer());
    }

    public static boolean isCrawling(UUID playerId) {
        return crawlingPlayers.contains(playerId);
    }

    /**
     * Re-applies swimming state every tick and updates crawl barrier (GSit-style).
     * Stops crawl when player is in water or flying (checkCrawlValid).
     */
    public void tickCrawl() {
        for (UUID id : new HashSet<>(crawlingPlayers)) {
            Player p = plugin.getServer().getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            if (p.isInWater() || p.isFlying()) {
                crawlingPlayers.remove(id);
                restoreCrawlBarrier(id);
                p.setSwimming(false);
                continue;
            }
            p.setSwimming(true);
            updateCrawlBarrierFor(p);
        }
    }

    /**
     * Syncs the chair armor stand's yaw and pitch to the player's look direction every tick,
     * so the player can rotate 360° smoothly on slabs/stairs without abrupt snaps.
     */
    public void tickChairRotation() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.isInsideVehicle()) continue;
            org.bukkit.entity.Entity vehicle = p.getVehicle();
            if (vehicle == null || !chairStands.contains(vehicle.getUniqueId())) continue;
            Location standLoc = vehicle.getLocation().clone();
            standLoc.setYaw(p.getLocation().getYaw());
            standLoc.setPitch(p.getLocation().getPitch());
            vehicle.teleport(standLoc);
        }
    }

    private void updateCrawlBarrierFor(Player player) {
        Location to = player.getLocation();
        int blockSize = (int) ((to.getY() - to.getBlockY()) * 100);
        double barrierHeight = blockSize >= 40 ? 2.49 : 1.49;
        Block above = to.clone().add(0, barrierHeight, 0).getBlock();
        if (!above.getType().isAir()) {
            restoreCrawlBarrier(player.getUniqueId());
            return;
        }
        Location barrierLoc = above.getLocation();
        Location current = crawlBarrierAt.get(player.getUniqueId());
        if (current != null && current.getBlockX() == barrierLoc.getBlockX() && current.getBlockY() == barrierLoc.getBlockY() && current.getBlockZ() == barrierLoc.getBlockZ()) {
            return;
        }
        if (current != null) {
            player.sendBlockChange(current, current.getBlock().getBlockData());
        }
        crawlBarrierAt.put(player.getUniqueId(), barrierLoc);
        player.sendBlockChange(barrierLoc, Material.BARRIER.createBlockData());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        org.bukkit.entity.Entity vehicle = event.getDismounted();
        if (!chairStands.remove(vehicle.getUniqueId())) {
            return;
        }
        Location blockLoc = standToBlock.remove(vehicle.getUniqueId());
        if (blockLoc != null) blockToStand.remove(new BlockKey(blockLoc));
        vehicle.remove();

        ConfigManager config = plugin.getConfigManager();
        if (config.isReturnOnStand()) {
            Location previous = playerToPreviousLocation.remove(player.getUniqueId());
            if (previous != null) {
                player.teleport(previous);
            }
        } else if (blockLoc != null && blockLoc.getWorld() != null) {
            Block block = blockLoc.getWorld().getBlockAt(blockLoc);
            double topY = block.getBoundingBox().getMaxY();
            Location up = new Location(block.getWorld(), block.getX() + 0.5, topY, block.getZ() + 0.5);
            up.setYaw(player.getLocation().getYaw());
            up.setPitch(player.getLocation().getPitch());
            player.teleport(up);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getConfigManager().isStandWhenBlockBreaks()) {
            return;
        }
        Block block = event.getBlock();
        BlockKey key = new BlockKey(block);
        UUID standId = blockToStand.get(key);
        if (standId == null) return;
        ArmorStand stand = block.getWorld().getEntitiesByClass(ArmorStand.class).stream()
                .filter(as -> as.getUniqueId().equals(standId))
                .findFirst()
                .orElse(null);
        if (stand != null) {
            stand.eject();
            stand.remove();
        }
        chairStands.remove(standId);
        standToBlock.remove(standId);
        blockToStand.remove(key);
    }

    /**
     * Removes all chair armor stands (e.g. on plugin disable). Uses PDC to find all chairs, including ghosts.
     */
    public static void cleanupAllChairs(SimpleChairs plugin) {
        if (plugin != null) {
            plugin.getServer().getWorlds().forEach(world ->
                    world.getEntitiesByClass(ArmorStand.class).stream()
                            .filter(plugin::isChairStand)
                            .forEach(stand -> {
                                Location blockLoc = standToBlock.remove(stand.getUniqueId());
                                if (blockLoc != null) blockToStand.remove(new BlockKey(blockLoc));
                                chairStands.remove(stand.getUniqueId());
                                stand.eject();
                                stand.remove();
                            }));
        }
        chairStands.clear();
        standToBlock.clear();
        blockToStand.clear();
        playerToPreviousLocation.clear();
        doubleSneakCrawlTime.clear();
        for (UUID id : new HashSet<>(crawlingPlayers)) {
            Location at = crawlBarrierAt.remove(id);
            if (at != null) {
                Player p = plugin.getServer().getPlayer(id);
                if (p != null && p.isOnline()) {
                    p.sendBlockChange(at, at.getBlock().getBlockData());
                }
            }
        }
        crawlBarrierAt.clear();
        plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> crawlingPlayers.contains(p.getUniqueId()))
                .forEach(p -> p.setSwimming(false));
        crawlingPlayers.clear();
    }
}
