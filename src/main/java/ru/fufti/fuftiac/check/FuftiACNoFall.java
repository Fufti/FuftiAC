package ru.fufti.fuftiac.check;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACNoFall extends FuftiACCheck {

    public FuftiACNoFall(FuftiAC plugin) {
        super(plugin, "NoFall");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setNoFallBuffer(0);
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            data.setNoFallBuffer(0);
            return;
        }

        if (player.isGliding() && player.getInventory().getChestplate() != null
                && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
            data.setNoFallBuffer(0);
            return;
        }

        boolean groundClaimed = player.isOnGround() || data.clientOnGround();
        if (!groundClaimed) {
            data.setNoFallBuffer(Math.max(0, data.noFallBuffer() - 1));
            return;
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (FuftiACPlayerData.isSupportedOnGround(to) || hasGroundSupport(to)) {
            data.setNoFallBuffer(0);
            return;
        }

        double deltaY = data.deltaY();
        float fallDist = player.getFallDistance();

        if (deltaY < -0.15 && fallDist > 2.0f && data.airTicks() >= 3) {
            data.setNoFallBuffer(data.noFallBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 1);
            if (data.noFallBuffer() >= need) {
                flagMove(player, data, event, "FuftiACNoFall", "airTicks=" + data.airTicks()
                        + " dy=" + String.format(java.util.Locale.US, "%.2f", deltaY)
                        + " fall=" + String.format(java.util.Locale.US, "%.1f", fallDist));
                player.setFallDistance(Math.max(fallDist, 3.0f));
                data.setNoFallBuffer(need - 1);
            }
        }
    }

    private static boolean hasGroundSupport(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        for (double x = -0.35; x <= 0.35; x += 0.35) {
            for (double z = -0.35; z <= 0.35; z += 0.35) {
                for (double y = -0.5; y <= 0.1; y += 0.25) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    if (mat.isSolid()) {
                        return true;
                    }
                    String name = mat.name();
                    if (name.contains("FENCE") || name.contains("WALL") || name.contains("CARPET")
                            || name.contains("SLAB") || name.contains("STAIR") || name.contains("LADDER")
                            || name.contains("VINE") || name.contains("SCAFFOLD") || name.contains("WATER")
                            || name.contains("LAVA") || name.contains("WEB") || name.contains("BOAT")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
