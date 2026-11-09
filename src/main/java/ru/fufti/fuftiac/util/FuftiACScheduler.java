package ru.fufti.fuftiac.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class FuftiACScheduler {

    private static final boolean FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (Throwable ignored) {
        }
        FOLIA = folia;
    }

    private FuftiACScheduler() {
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static FuftiACTask runGlobalTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (FOLIA) {
            return FuftiACFoliaScheduler.runGlobalTimer(plugin, runnable, initialDelayTicks, periodTicks);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, initialDelayTicks, periodTicks);
        return () -> {
            if (task != null) {
                task.cancel();
            }
        };
    }

    public static void runTask(Plugin plugin, Runnable runnable) {
        if (FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runEntityTask(Plugin plugin, Entity entity, Runnable runnable) {
        if (FOLIA) {
            FuftiACFoliaScheduler.runEntityTask(plugin, entity, runnable);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static FuftiACTask runEntityTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        if (FOLIA) {
            return FuftiACFoliaScheduler.runEntityTaskLater(plugin, entity, runnable, delayTicks);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        return () -> {
            if (task != null) {
                task.cancel();
            }
        };
    }

    public static FuftiACTask runEntityTaskTimer(Plugin plugin, Entity entity, Runnable runnable, long delayTicks, long periodTicks) {
        if (FOLIA) {
            return FuftiACFoliaScheduler.runEntityTaskTimer(plugin, entity, runnable, delayTicks, periodTicks);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        return () -> {
            if (task != null) {
                task.cancel();
            }
        };
    }
}
