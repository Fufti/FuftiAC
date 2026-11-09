package ru.fufti.fuftiac.check;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACJesus extends FuftiACCheck {

    public FuftiACJesus(FuftiAC plugin) {
        super(plugin, "Jesus");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setJesusBuffer(0);
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            data.setJesusBuffer(0);
            return;
        }

        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            data.setJesusBuffer(0);
            return;
        }

        Location loc = event.getTo();
        if (loc == null) {
            return;
        }

        Block feetBlock = loc.getBlock();
        Material feetMat = feetBlock.getType();
        Block belowBlock = loc.clone().subtract(0, 0.35, 0).getBlock();
        Material belowMat = belowBlock.getType();

        boolean inLiquid = isLiquid(feetMat);
        boolean aboveLiquid = isLiquid(belowMat) && feetMat.isAir();

        if (!inLiquid && !aboveLiquid) {
            data.setJesusBuffer(Math.max(0, data.jesusBuffer() - 1));
            return;
        }

        if (hasSolidSupportNearby(loc)) {
            data.setJesusBuffer(Math.max(0, data.jesusBuffer() - 1));
            return;
        }

        double speed = data.speed();
        double deltaY = data.deltaY();

        int depthStrider = getDepthStriderLevel(player);
        boolean dolphinsGrace = player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE);
        boolean speedEffect = player.hasPotionEffect(PotionEffectType.SPEED);

        double maxWaterSpeed = 0.19 + (depthStrider * 0.08) + (dolphinsGrace ? 0.20 : 0) + (speedEffect ? 0.05 : 0);

        boolean illegal = false;
        if (aboveLiquid) {
            if (data.clientOnGround() || player.isOnGround()) {
                illegal = true;
            } else if (speed > 0.20 && Math.abs(deltaY) < 0.08) {
                illegal = true;
            }
        } else if (inLiquid) {
            if (speed > maxWaterSpeed) {
                illegal = true;
            }
        }

        if (illegal) {
            data.setJesusBuffer(data.jesusBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.jesusBuffer() >= need) {
                flagMove(player, data, event, "FuftiACJesus",
                        "speed=" + String.format(java.util.Locale.US, "%.2f", speed) + " dy=" + String.format(java.util.Locale.US, "%.2f", deltaY));
                data.setJesusBuffer(need - 1);
            }
        } else {
            data.setJesusBuffer(Math.max(0, data.jesusBuffer() - 1));
        }
    }

    private static int getDepthStriderLevel(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        if (boots != null && boots.hasItemMeta()) {
            try {
                return boots.getEnchantmentLevel(Enchantment.DEPTH_STRIDER);
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    private static boolean isLiquid(Material mat) {
        return mat == Material.WATER || mat == Material.LAVA;
    }

    private static boolean hasSolidSupportNearby(Location loc) {
        for (double x = -0.35; x <= 0.35; x += 0.35) {
            for (double z = -0.35; z <= 0.35; z += 0.35) {
                for (double y = -0.4; y <= 0.2; y += 0.3) {
                    Material mat = loc.clone().add(x, y, z).getBlock().getType();
                    if (mat.isSolid() || mat == Material.LILY_PAD || mat.name().contains("CARPET") || mat.name().contains("BOAT")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
