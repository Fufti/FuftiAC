package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;

public final class FuftiACAirStuck extends FuftiACCheck {

    public FuftiACAirStuck(FuftiAC plugin) {
        super(plugin, "AirStuck");
    }

    public void checkMove(Player player, FuftiACPlayerData data, Location from, Location to, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setAirStuckBuffer(0);
            return;
        }

        if (player.getAllowFlight() || player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            data.setAirStuckBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastDamageMs() < 600L || data.hasPendingVelocity() || now - data.lastVelocityMs() < 600L) {
            data.setAirStuckBuffer(0);
            return;
        }

        if (player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            data.setAirStuckBuffer(0);
            return;
        }

        if (FuftiACPlayerData.isSupportedOnGround(to) || hasNearbySupport(to)) {
            data.setAirStuckBuffer(0);
            return;
        }

        int ping = FuftiACCombatUtil.getPing(player);
        if (ping > 300) {
            data.setAirStuckBuffer(Math.max(0, data.airStuckBuffer() - 1));
            return;
        }

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horiz = Math.hypot(dx, dz);

        if (Math.abs(dy) < 0.008 && horiz < 0.03 && data.airTicks() >= 3) {
            data.setAirStuckBuffer(data.airStuckBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.airStuckBuffer() >= need) {
                flagMove(player, data, event, "FuftiACAirStuck", "frozen in air airTicks=" + data.airTicks() + " ping=" + ping);
                player.setVelocity(new Vector(0, -0.6, 0));
                data.setAirStuckBuffer(need - 1);
            }
        } else {
            data.setAirStuckBuffer(Math.max(0, data.airStuckBuffer() - 1));
        }
    }

    private static boolean hasNearbySupport(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        for (double x = -0.4; x <= 0.4; x += 0.4) {
            for (double z = -0.4; z <= 0.4; z += 0.4) {
                for (double y = -0.5; y <= 0.3; y += 0.3) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    if (mat.isSolid()) {
                        return true;
                    }
                    String name = mat.name();
                    if (name.contains("WATER") || name.contains("LAVA") || name.contains("LADDER")
                            || name.contains("VINE") || name.contains("SCAFFOLD") || name.contains("WEB")
                            || name.contains("COBWEB") || name.contains("BUBBLE") || name.contains("SLIME")
                            || name.contains("HONEY") || name.contains("CLIMBABLE")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
