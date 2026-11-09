package ru.fufti.fuftiac.check;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACFly extends FuftiACCheck {

    public FuftiACFly(FuftiAC plugin) {
        super(plugin, "Fly");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setFlyBuffer(0);
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            data.setFlyBuffer(0);
            return;
        }

        if (player.isGliding()) {
            org.bukkit.inventory.ItemStack chest = player.getInventory().getChestplate();
            if (chest != null && chest.getType() == Material.ELYTRA) {
                data.setFlyBuffer(0);
                return;
            }
            flagMove(player, data, event, "FuftiACFly", "gliding without elytra");
            return;
        }

        if (player.isFlying()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            flagMove(player, data, event, "FuftiACFly", "flying in survival");
            return;
        }

        if (player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            data.setFlyBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastDamageMs() < 550L) {
            data.setFlyBuffer(0);
            return;
        }

        if (inLiquidOrClimbable(player)) {
            data.setFlyBuffer(0);
            return;
        }

        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || from == null) {
            return;
        }

        boolean supportedTo = FuftiACPlayerData.isSupportedOnGround(to);

        if (supportedTo) {
            data.setFlyBuffer(Math.max(0, data.flyBuffer() - 1));
            return;
        }

        double deltaY = data.deltaY();
        double speed = data.speed();
        int airTicks = data.airTicks();

        boolean airWalk = deltaY > -0.07 && deltaY <= 0.05 && speed > 0.08 && airTicks >= 3;
        boolean hovering = Math.abs(deltaY) < 0.005 && airTicks >= 3;
        boolean rising = deltaY > 0.05 && airTicks >= 8;
        boolean vulcanRise = deltaY > 0.15 && airTicks >= 7;
        boolean glide = deltaY < -0.001 && deltaY > -0.15 && speed > 0.22 && airTicks >= 6;
        boolean groundSpoof = (data.clientOnGround() || player.isOnGround()) && !supportedTo && (speed > 0.10 || airTicks >= 3);
        boolean fastAir = speed > 0.65 && airTicks >= 2;

        if (airWalk || hovering || rising || vulcanRise || glide || groundSpoof || fastAir) {
            data.setFlyBuffer(data.flyBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.flyBuffer() >= need) {
                flagMove(player, data, event, "FuftiACFly",
                        "dy=" + String.format(java.util.Locale.US, "%.3f", deltaY) + " speed=" + String.format(java.util.Locale.US, "%.2f", speed) + " airTicks=" + airTicks);
                data.setFlyBuffer(need - 1);
            }
        } else {
            data.setFlyBuffer(Math.max(0, data.flyBuffer() - 1));
        }
    }

    private static boolean inLiquidOrClimbable(Player player) {
        Location loc = player.getLocation();
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                for (double y = -0.3; y <= 0.8; y += 0.4) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    if (mat.isAir()) {
                        continue;
                    }
                    String name = mat.name();
                    if (mat == Material.WATER || mat == Material.LAVA || name.contains("BUBBLE")
                            || name.contains("LADDER") || name.contains("VINE") || name.contains("SCAFFOLD")
                            || name.contains("WEB")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
