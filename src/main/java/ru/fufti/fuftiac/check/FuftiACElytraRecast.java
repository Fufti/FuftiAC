package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACElytraRecast extends FuftiACCheck {

    public FuftiACElytraRecast(FuftiAC plugin) {
        super(plugin, "ElytraRecast");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setElytraRecastBuffer(0);
            return;
        }

        if (player.getAllowFlight() || player.isFlying()) {
            data.setElytraRecastBuffer(0);
            return;
        }

        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null || chest.getType() != Material.ELYTRA) {
            data.setElytraRecastBuffer(0);
            return;
        }

        if (player.isOnGround() || data.onGround() || FuftiACPlayerData.isSupportedOnGround(player.getLocation())) {
            data.setElytraRecastBuffer(0);
            return;
        }

        if (hasGroundBelow(player.getLocation(), 1.8)) {
            data.setElytraRecastBuffer(0);
            return;
        }

        if (player.isRiptiding() || player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            data.setElytraRecastBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastDamageMs() < 1500L || data.hasPendingVelocity() || now - data.lastVelocityMs() < 1500L) {
            data.setElytraRecastBuffer(0);
            return;
        }

        if (data.airTicks() < 8 && !player.isGliding()) {
            data.setElytraRecastBuffer(0);
            return;
        }

        double deltaY = data.deltaY();
        if (deltaY <= 0.18) {
            data.setElytraRecastBuffer(Math.max(0, data.elytraRecastBuffer() - 1));
            return;
        }

        Location loc = player.getLocation();
        if (nearClimbOrBubble(loc)) {
            data.setElytraRecastBuffer(0);
            return;
        }

        double speed = data.speed();
        boolean midairAscend = deltaY > 0.22 && speed < 0.55;

        if (midairAscend) {
            data.setElytraRecastBuffer(data.elytraRecastBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.elytraRecastBuffer() >= need) {
                flagMove(player, data, event, "FuftiACElytraRecast", "ascend dy=" + String.format(java.util.Locale.US, "%.2f", deltaY)
                        + " speed=" + String.format(java.util.Locale.US, "%.2f", speed)
                        + " airTicks=" + data.airTicks());
                data.setElytraRecastBuffer(need - 1);
            }
        } else {
            data.setElytraRecastBuffer(Math.max(0, data.elytraRecastBuffer() - 1));
        }
    }

    private static boolean hasGroundBelow(Location loc, double depth) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        for (double d = 0.2; d <= depth; d += 0.4) {
            for (double x = -0.3; x <= 0.3; x += 0.3) {
                for (double z = -0.3; z <= 0.3; z += 0.3) {
                    Block b = loc.clone().subtract(x, d, z).getBlock();
                    Material mat = b.getType();
                    if (mat.isSolid()) {
                        return true;
                    }
                    String name = mat.name();
                    if (name.contains("SLAB") || name.contains("STAIR") || name.contains("CARPET")
                            || name.contains("WATER") || name.contains("LAVA")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean nearClimbOrBubble(Location loc) {
        for (double x = -0.4; x <= 0.4; x += 0.4) {
            for (double z = -0.4; z <= 0.4; z += 0.4) {
                for (double y = -1.0; y <= 0.5; y += 0.5) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    String name = mat.name();
                    if (mat == Material.WATER || mat == Material.LAVA || name.contains("BUBBLE")
                            || name.contains("LADDER") || name.contains("VINE") || name.contains("SCAFFOLD")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
