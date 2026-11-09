package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACStep extends FuftiACCheck {

    public FuftiACStep(FuftiAC plugin) {
        super(plugin, "Step");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setStepBuffer(0);
            return;
        }

        if (player.getAllowFlight() || player.isFlying() || player.isGliding()) {
            data.setStepBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastDamageMs() < 1000L || data.hasPendingVelocity() || now - data.lastVelocityMs() < 1000L) {
            data.setStepBuffer(0);
            return;
        }

        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            data.setStepBuffer(0);
            return;
        }

        double maxStep = 0.62;
        PotionEffect jumpEffect = player.getPotionEffect(PotionEffectType.JUMP_BOOST);
        if (jumpEffect != null) {
            maxStep += (jumpEffect.getAmplifier() + 1) * 0.15;
        }

        double deltaY = data.deltaY();
        if (deltaY <= maxStep) {
            data.setStepBuffer(Math.max(0, data.stepBuffer() - 1));
            return;
        }

        Location loc = player.getLocation();
        if (nearBouncyOrBubble(loc)) {
            data.setStepBuffer(0);
            return;
        }

        data.setStepBuffer(data.stepBuffer() + 1);
        int need = this.plugin.fuftiConfig().integer(name(), "buffer", 1);
        if (data.stepBuffer() >= need) {
            flagMove(player, data, event, "FuftiACStep", "dy=" + String.format(java.util.Locale.US, "%.2f", deltaY)
                    + " max=" + String.format(java.util.Locale.US, "%.2f", maxStep));
            data.setStepBuffer(need - 1);
        }
    }

    private static boolean nearBouncyOrBubble(Location loc) {
        for (double x = -0.4; x <= 0.4; x += 0.4) {
            for (double z = -0.4; z <= 0.4; z += 0.4) {
                for (double y = -1.0; y <= 0.5; y += 0.5) {
                    Material mat = loc.clone().add(x, y, z).getBlock().getType();
                    String name = mat.name();
                    if (name.contains("SLIME") || name.contains("BED") || name.contains("BUBBLE")
                            || name.contains("PISTON") || name.contains("SCAFFOLD")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
