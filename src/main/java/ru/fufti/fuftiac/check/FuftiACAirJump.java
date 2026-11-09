package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACAirJump extends FuftiACCheck {

    public FuftiACAirJump(FuftiAC plugin) {
        super(plugin, "AirJump");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setAirJumpBuffer(0);
            return;
        }

        if (player.getAllowFlight() || player.isFlying() || player.isGliding()) {
            data.setAirJumpBuffer(0);
            return;
        }

        if (player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            data.setAirJumpBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastDamageMs() < 1200L || data.hasPendingVelocity() || now - data.lastVelocityMs() < 1200L
                || now - data.lastElytraSwapTime() < 2500L || now - data.lastVanillaElytraEquip() < 2500L) {
            data.setAirJumpBuffer(0);
            return;
        }

        double deltaY = data.deltaY();
        if (deltaY <= 0.15) {
            data.setAirJumpBuffer(Math.max(0, data.airJumpBuffer() - 1));
            return;
        }

        Location loc = player.getLocation();
        if (nearClimbableOrLiquidOrBouncy(loc)) {
            data.setAirJumpBuffer(0);
            return;
        }

        int jumpAmp = getJumpAmplifier(player);
        double maxNormalJump = 0.65 + (jumpAmp * 0.18);

        boolean onGround = player.isOnGround() || data.onGround() || FuftiACPlayerData.isSupportedOnGround(loc);
        boolean groundBelow = hasGroundBelow(loc, 1.25);

        if (onGround || groundBelow) {
            if (deltaY > maxNormalJump) {
                data.setAirJumpBuffer(data.airJumpBuffer() + 1);
                int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
                if (data.airJumpBuffer() >= need) {
                    flagMove(player, data, event, "FuftiACAirJump", "high jump dy=" + String.format(java.util.Locale.US, "%.2f", deltaY)
                            + " max=" + String.format(java.util.Locale.US, "%.2f", maxNormalJump));
                    data.setAirJumpBuffer(need - 1);
                }
            } else {
                data.setAirJumpBuffer(0);
            }
            return;
        }

        double lastDeltaY = data.lastDeltaY();
        boolean suddenMidAirJump = deltaY > 0.32 && lastDeltaY < 0.10 && data.airTicks() >= 3;
        boolean persistentAirRise = deltaY > 0.15 && data.airTicks() >= 6;

        if (suddenMidAirJump || persistentAirRise) {
            data.setAirJumpBuffer(data.airJumpBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.airJumpBuffer() >= need) {
                flagMove(player, data, event, "FuftiACAirJump", "dy=" + String.format(java.util.Locale.US, "%.2f", deltaY)
                        + " airTicks=" + data.airTicks());
                data.setAirJumpBuffer(need - 1);
            }
        } else {
            data.setAirJumpBuffer(Math.max(0, data.airJumpBuffer() - 1));
        }
    }

    private static int getJumpAmplifier(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        return effect != null ? effect.getAmplifier() + 1 : 0;
    }

    private static boolean hasGroundBelow(Location loc, double depth) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        for (double d = 0.1; d <= depth; d += 0.3) {
            for (double x = -0.3; x <= 0.3; x += 0.3) {
                for (double z = -0.3; z <= 0.3; z += 0.3) {
                    Block b = loc.clone().subtract(x, d, z).getBlock();
                    Material mat = b.getType();
                    if (mat.isSolid()) {
                        return true;
                    }
                    String name = mat.name();
                    if (name.contains("SLAB") || name.contains("STAIR") || name.contains("CARPET")
                            || name.contains("SNOW") || name.contains("PATH") || name.contains("FARMLAND")
                            || name.contains("FENCE") || name.contains("WALL") || name.contains("BED")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean nearClimbableOrLiquidOrBouncy(Location loc) {
        for (double x = -0.4; x <= 0.4; x += 0.4) {
            for (double z = -0.4; z <= 0.4; z += 0.4) {
                for (double y = -0.5; y <= 0.8; y += 0.4) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    if (mat.isAir()) {
                        continue;
                    }
                    String name = mat.name();
                    if (mat == Material.WATER || mat == Material.LAVA || name.contains("LADDER")
                            || name.contains("VINE") || name.contains("SCAFFOLD") || name.contains("BUBBLE")
                            || name.contains("SLIME") || name.contains("BED") || name.contains("WEB")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
