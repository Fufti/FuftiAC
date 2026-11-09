package ru.fufti.fuftiac.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

final class FuftiACFoliaScheduler {

    private FuftiACFoliaScheduler() {
    }

    static FuftiACTask runGlobalTimer(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        long init = Math.max(1L, initialDelayTicks);
        long period = Math.max(1L, periodTicks);
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> runnable.run(), init, period);
        return () -> {
            if (task != null) {
                task.cancel();
            }
        };
    }

    static void runEntityTask(Plugin plugin, Entity entity, Runnable runnable) {
        if (entity == null || !entity.isValid()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
            return;
        }
        entity.getScheduler().run(plugin, t -> runnable.run(), null);
    }

    static FuftiACTask runEntityTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delayTicks) {
        long delay = Math.max(1L, delayTicks);
        if (entity == null || !entity.isValid()) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), delay);
            return () -> {
                if (task != null) {
                    task.cancel();
                }
            };
        }
        ScheduledTask task = entity.getScheduler().runDelayed(plugin, t -> runnable.run(), null, delay);
        return () -> {
            if (task != null) {
                task.cancel();
            }
        };
    }

    static FuftiACTask runEntityTaskTimer(Plugin plugin, Entity entity, Runnable runnable, long delayTicks, long periodTicks) {
        long delay = Math.max(1L, delayTicks);
        long period = Math.max(1L, periodTicks);
        if (entity == null || !entity.isValid()) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> runnable.run(), delay, period);
            return () -> {
                if (task != null) {
                    task.cancel();
                }
            };
        }
        ScheduledTask task = entity.getScheduler().runAtFixedRate(plugin, t -> runnable.run(), null, delay, period);
        return () -> {
            if (task != null) {
                task.cancel();
            }
        };
    }
}
