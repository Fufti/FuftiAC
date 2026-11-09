package ru.fufti.fuftiac.check;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACVelocityLegit extends FuftiACCheck {

    public FuftiACVelocityLegit(FuftiAC plugin) {
        super(plugin, "Velocity Legit");
    }

    public void checkVelocity(Player player, FuftiACPlayerData data, Vector expected, double actualHoriz) {
        if (exempt(player, data) || !enabled()) {
            data.setVelocityLegitBuffer(0);
            return;
        }

        double expHoriz = Math.hypot(expected.getX(), expected.getZ());
        if (expHoriz < 0.22) {
            return;
        }

        if (FuftiACVelocity.hasObstructionInDir(data.velocityStartLoc(), player.getLocation(), expected)) {
            data.setVelocityLegitBuffer(Math.max(0, data.velocityLegitBuffer() - 1));
            return;
        }

        double ratio = actualHoriz / expHoriz;
        if (ratio < 0.65 && ratio >= 0.04) {
            data.setVelocityLegitBuffer(data.velocityLegitBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.velocityLegitBuffer() >= need) {
                flag(player, data, "FuftiACVelocity Legit", "ratio=" + String.format(java.util.Locale.US, "%.2f", ratio)
                        + " exp=" + String.format(java.util.Locale.US, "%.2f", expHoriz)
                        + " act=" + String.format(java.util.Locale.US, "%.2f", actualHoriz));
                data.setVelocityLegitBuffer(need - 1);
            }
        } else if (ratio >= 0.85) {
            data.setVelocityLegitBuffer(Math.max(0, data.velocityLegitBuffer() - 1));
        }
    }
}
