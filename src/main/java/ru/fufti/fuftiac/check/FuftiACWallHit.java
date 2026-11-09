package ru.fufti.fuftiac.check;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;

public final class FuftiACWallHit extends FuftiACCheck {

    public FuftiACWallHit(FuftiAC plugin) {
        super(plugin, "WallHit");
    }

    public boolean handleAttack(Player player, LivingEntity target, EntityDamageByEntityEvent event) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !enabled()) {
            return false;
        }
        double min = this.plugin.fuftiConfig().decimal(name(), "min-distance", 1.25);
        if (FuftiACCombatUtil.reach(player, target) < min) {
            data.setWallBuffer(Math.max(0, data.wallBuffer() - 1));
            return false;
        }
        if (FuftiACCombatUtil.wallBetween(player, target)) {
            data.setWallBuffer(data.wallBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.wallBuffer() >= need) {
                flag(player, data, "FuftiACWallHit", "through wall");
                data.setWallBuffer(need - 1);
            }
            if (event != null) {
                event.setCancelled(true);
            }
            return true;
        } else {
            data.setWallBuffer(Math.max(0, data.wallBuffer() - 1));
            return false;
        }
    }
}
