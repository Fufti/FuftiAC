package ru.fufti.fuftiac.check;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

import java.util.UUID;

public final class FuftiACSpeed extends FuftiACCheck {

    private static final long PACKET_NS = 50_000_000L;

    public FuftiACSpeed(FuftiAC plugin) {
        super(plugin, "Speed");
    }

    public void onFlyingPacket(UUID uuid) {
        FuftiACPlayerData data = this.plugin.data().get(uuid);
        long now = System.nanoTime();
        long nowMs = System.currentTimeMillis();
        long last = data.timerLastNs().getAndSet(now);
        if (last == 0L) {
            data.setTimerWindowStartMs(nowMs);
            data.setFlyingPacketsInWindow(0);
            return;
        }

        if (nowMs - data.timerWindowStartMs() >= 1000L) {
            data.setTimerWindowStartMs(nowMs);
            data.setFlyingPacketsInWindow(0);
        }
        int packets = data.incrementFlyingPacketsInWindow();
        if (packets >= 28) {
            data.setTimerBurstDetected(true);
        }

        long elapsed = now - last;
        if (elapsed > 2_000_000_000L) {
            data.timerBalanceNs().set(0L);
            return;
        }

        long diff = PACKET_NS - elapsed;
        long current = data.timerBalanceNs().get();
        long next = Math.max(-1_000_000_000L, Math.min(1_000_000_000L, current + diff));
        data.timerBalanceNs().set(next);

        if (next > 400_000_000L) {
            data.timerBalanceNs().set(100_000_000L);
            data.setTimerBurstDetected(true);
        }
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setSpeedBuffer(0);
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            data.setSpeedBuffer(0);
            return;
        }

        if (player.isGliding() && player.getInventory().getChestplate() != null
                && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
            data.setSpeedBuffer(0);
            return;
        }

        if (player.isInsideVehicle()) {
            data.setSpeedBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        boolean recentDamage = (now - data.lastDamageMs() < 550L) || data.hasPendingVelocity() || (now - data.lastVelocityMs() < 550L);
        if (now - data.lastElytraSwapTime() < 2500L || now - data.lastVanillaElytraEquip() < 2500L) {
            data.setSpeedBuffer(0);
            return;
        }

        double speed = data.speed();
        double deltaY = data.deltaY();
        boolean onGround = data.onGround();
        int speedAmp = getSpeedAmplifier(player);

        double hardLimit = onGround ? (0.65 + (speedAmp * 0.15)) : (0.85 + (speedAmp * 0.20));
        if (speed > hardLimit && (!recentDamage || speed > 1.25)) {
            flagMove(player, data, event, "FuftiACSpeed", "speed=" + round(speed) + " max=" + round(hardLimit));
            data.setSpeedBuffer(0);
            return;
        }

        if (recentDamage || onSlipperyBlock(player) || inLiquid(player)) {
            data.setSpeedBuffer(Math.max(0, data.speedBuffer() - 1));
            return;
        }

        double maxFriction = (player.isSprinting() ? 0.365 : 0.28) + (speedAmp * 0.08);
        double maxAir = 0.58 + (speedAmp * 0.12);

        boolean groundExceeded = onGround && speed > maxFriction;
        boolean airExceeded = !onGround && speed > maxAir;
        boolean bhopPattern = !onGround && deltaY > 0.35 && speed > 0.48 + (speedAmp * 0.08);

        if (groundExceeded || airExceeded || bhopPattern) {
            data.setSpeedBuffer(data.speedBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 3);
            if (data.speedBuffer() >= need) {
                flagMove(player, data, event, "FuftiACSpeed", "speed=" + round(speed) + " dy=" + round(deltaY) + " ground=" + onGround);
                data.setSpeedBuffer(need - 1);
            }
        } else {
            data.setSpeedBuffer(Math.max(0, data.speedBuffer() - 1));
        }

        if (data.timerBurstDetected()) {
            data.setTimerBurstDetected(false);
            flagMove(player, data, event, "FuftiACSpeed", "timer packets=" + data.flyingPacketsInWindow() + "/s");
            data.setFlyingPacketsInWindow(0);
        }
    }

    private static boolean onSlipperyBlock(Player player) {
        Block below = player.getLocation().getBlock().getRelative(0, -1, 0);
        Material type = below.getType();
        return type == Material.ICE
                || type == Material.PACKED_ICE
                || type == Material.BLUE_ICE
                || type == Material.SLIME_BLOCK;
    }

    private static boolean inLiquid(Player player) {
        Material type = player.getLocation().getBlock().getType();
        return type == Material.WATER || type == Material.LAVA;
    }

    private static int getSpeedAmplifier(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.SPEED);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    private static String round(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
