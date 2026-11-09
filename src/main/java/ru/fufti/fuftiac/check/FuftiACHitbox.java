package ru.fufti.fuftiac.check;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;

public final class FuftiACHitbox extends FuftiACCheck {

    public FuftiACHitbox(FuftiAC plugin) {
        super(plugin, "Hitbox");
    }

    public boolean handleAttack(Player player, LivingEntity target, EntityDamageByEntityEvent event) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !enabled()) {
            return false;
        }

        double missDist = FuftiACCombatUtil.rayMissDistance(player, target);
        int ping = FuftiACCombatUtil.getPing(player);
        double allowedOffset = 0.75 + Math.min(0.50, (ping / 1000.0) * 0.60);

        if (missDist > allowedOffset) {
            data.setHitboxBuffer(data.hitboxBuffer() + 1);
            if (data.hitboxBuffer() >= 4) {
                flag(player, data, "FuftiACHitbox", "aim miss=" + String.format(java.util.Locale.US, "%.3f", missDist) + "m");
                data.setHitboxBuffer(3);
            }
            if (event != null) {
                event.setCancelled(true);
            }
            return true;
        } else {
            data.setHitboxBuffer(Math.max(0, data.hitboxBuffer() - 1));
            return false;
        }
    }
}
