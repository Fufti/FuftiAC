package ru.fufti.fuftiac.check;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACNoSlow extends FuftiACCheck {

    public FuftiACNoSlow(FuftiAC plugin) {
        super(plugin, "NoSlow");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled() || player.isFlying() || player.isGliding() || player.isInsideVehicle()) {
            data.setNoSlowBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastDamageMs() < 850L || now - data.lastConsumeMs() < 800L) {
            data.setNoSlowBuffer(0);
            return;
        }

        if (onSlipperyBlock(player)) {
            data.setNoSlowBuffer(Math.max(0, data.noSlowBuffer() - 1));
            return;
        }

        double speed = data.speed();
        int speedAmp = getSpeedAmplifier(player);
        double maxAllowedWalk = 0.13 + (speedAmp * 0.035);

        boolean isSneak = player.isSneaking();
        double maxAllowedSneak = 0.08 + (speedAmp * 0.02);

        boolean itemUse = isUsingItem(player);
        boolean sneakExceeded = isSneak && speed > maxAllowedSneak;
        boolean itemExceeded = itemUse && speed > maxAllowedWalk;

        if (!itemUse && !isSneak) {
            data.setNoSlowBuffer(Math.max(0, data.noSlowBuffer() - 1));
            return;
        }

        String subType = null;
        if (itemExceeded) {
            if (player.isBlocking()) {
                subType = "FuftiACNoSlow Shield";
            } else {
                org.bukkit.inventory.ItemStack item = player.getInventory().getItemInMainHand();
                if (item == null || item.getType().isAir()) {
                    item = player.getInventory().getItemInOffHand();
                }
                if (item != null && !item.getType().isAir()) {
                    Material m = item.getType();
                    if (m == Material.BOW || m == Material.CROSSBOW) {
                        subType = "FuftiACNoSlow Bow";
                    } else if (m.isEdible() || m == Material.POTION || m == Material.MILK_BUCKET || m == Material.HONEY_BOTTLE) {
                        subType = "FuftiACNoSlow Consume";
                    } else {
                        subType = "FuftiACNoSlow Legit";
                    }
                } else {
                    subType = "FuftiACNoSlow Legit";
                }
            }
        } else if (sneakExceeded) {
            subType = "FuftiACNoSlow Sneak";
        }

        if (subType != null) {
            data.setNoSlowBuffer(data.noSlowBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.noSlowBuffer() >= need) {
                double maxVal = sneakExceeded ? maxAllowedSneak : maxAllowedWalk;
                flagMove(player, data, event, "FuftiACNoSlow",
                        "speed=" + String.format(java.util.Locale.US, "%.3f", speed) + " max=" + String.format(java.util.Locale.US, "%.3f", maxVal));
                data.setNoSlowBuffer(1);
            }
        } else {
            data.setNoSlowBuffer(Math.max(0, data.noSlowBuffer() - 1));
        }
    }

    private static boolean onSlipperyBlock(Player player) {
        Block below = player.getLocation().getBlock().getRelative(0, -1, 0);
        Material type = below.getType();
        return type == Material.ICE
                || type == Material.PACKED_ICE
                || type == Material.BLUE_ICE
                || type == Material.SLIME_BLOCK
                || type == Material.SOUL_SAND
                || type.name().contains("SOUL");
    }

    private static int getSpeedAmplifier(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.SPEED);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    private static boolean isUsingItem(Player player) {
        if (player.isBlocking()) {
            return true;
        }
        try {
            return player.isHandRaised();
        } catch (Throwable ignored) {
        }
        return false;
    }
}
