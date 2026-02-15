package com.simpleplugins.SimpleChairs;

import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;

/**
 * Optional WorldGuard integration via reflection, so the plugin builds without
 * the WorldGuard dependency. Only used when WorldGuard is present and
 * worldguard-integration is enabled. Registers the "chairs-deny" flag and checks it at sit location.
 */
public class WorldGuardIntegration {

    private final Object regionContainer;
    private final Method getManagerMethod;
    private final Object chairsDenyFlag;

    public WorldGuardIntegration(SimpleChairs plugin) throws Exception {
        Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
        Object platform = worldGuard.getClass().getMethod("getPlatform").invoke(worldGuard);
        regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);
        getManagerMethod = regionContainer.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World"));
        chairsDenyFlag = registerOrGetFlag(worldGuard);
    }

    private Object registerOrGetFlag(Object worldGuard) throws Exception {
        Object flagRegistry = worldGuard.getClass().getMethod("getFlagRegistry").invoke(worldGuard);
        Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
        Object newFlag = stateFlagClass.getConstructor(String.class, boolean.class).newInstance("chairs-deny", false);
        try {
            flagRegistry.getClass().getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag")).invoke(flagRegistry, newFlag);
            return newFlag;
        } catch (Exception e) {
            Object existing = flagRegistry.getClass().getMethod("get", String.class).invoke(flagRegistry, "chairs-deny");
            return existing != null ? existing : newFlag;
        }
    }

    /**
     * Returns true if sitting should be denied at the given location (region has chairs-deny = allow).
     */
    public boolean isChairDenied(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        try {
            World world = location.getWorld();
            Object weWorld = adaptWorld(world);
            if (weWorld == null) return false;
            Object manager = getManagerMethod.invoke(regionContainer, weWorld);
            if (manager == null) return false;
            Class<?> blockVector3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            Object blockVector3 = blockVector3Class
                    .getMethod("at", int.class, int.class, int.class)
                    .invoke(null, location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Method getApplicable = manager.getClass().getMethod("getApplicableRegions", blockVector3Class);
            Object regionSet = getApplicable.invoke(manager, blockVector3);
            if (regionSet == null) return false;
            Boolean isVirtual = (Boolean) regionSet.getClass().getMethod("isVirtual").invoke(regionSet);
            if (Boolean.TRUE.equals(isVirtual)) return false;
            Class<?> regionAssociableClass = Class.forName("com.sk89q.worldguard.protection.association.RegionAssociable");
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            Method testState = regionSet.getClass().getMethod("testState", regionAssociableClass, stateFlagClass);
            return Boolean.TRUE.equals(testState.invoke(regionSet, null, chairsDenyFlag));
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object adaptWorld(World bukkitWorld) {
        try {
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adapt = bukkitAdapter.getMethod("adapt", org.bukkit.World.class);
            return adapt.invoke(null, bukkitWorld);
        } catch (Throwable t) {
            return null;
        }
    }
}
