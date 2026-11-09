package ru.fufti.fuftiac;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACJoinListener implements Listener {

    private final FuftiAC plugin;

    public FuftiACJoinListener(FuftiAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FuftiACPlayerData data = this.plugin.data().get(player);
        data.setAlerts(true);
        data.setPunishing(false);
        data.setLastValidLocation(player.getLocation());
        this.plugin.punish().resetMovement(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.plugin.punish().resetMovement(event.getPlayer());
        this.plugin.checks().npc().remove(event.getPlayer());
        this.plugin.data().remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        this.plugin.punish().resetMovement(player);
        FuftiACPlayerData data = this.plugin.data().get(player);
        data.setAirTicks(0);
        data.setOnGround(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        this.plugin.punish().resetMovement(player);
        FuftiACPlayerData data = this.plugin.data().get(player);
        data.setAirTicks(0);
        data.setOnGround(true);
        data.setLastValidLocation(event.getRespawnLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        FuftiACPlayerData data = this.plugin.data().get(player);
        data.setLastValidLocation(event.getTo());
        data.setAirTicks(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (data.punishing()) {
            event.setCancelled(true);
            return;
        }
        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            if (!player.hasPermission("fuftiac.bypass")) {
                event.setCancelled(true);
                player.setFlying(false);
                player.setAllowFlight(false);
                this.plugin.checks().fly().flag(player, data, "FuftiACFly", "flight toggle in survival");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        FuftiACPlayerData data = this.plugin.data().get(event.getPlayer());
        data.markConsume();
        data.setNoSlowBuffer(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                    || cause == EntityDamageEvent.DamageCause.PROJECTILE
                    || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                this.plugin.data().get(player).markDamage();
            }
        }
    }
}
