package ru.fufti.fuftiac.check;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;

public final class FuftiACReach extends FuftiACCheck {

    public FuftiACReach(FuftiAC plugin) {
        super(plugin, "Reach");
    }

    public boolean handleAttack(Player player, LivingEntity target, EntityDamageByEntityEvent event) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !enabled()) {
            return false;
        }

        double reach = FuftiACCombatUtil.reach(player, target);
        double max = FuftiACCombatUtil.maxReach(player, this.plugin, name());

        if (reach > max) {
            data.setReachBuffer(data.reachBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
            if (data.reachBuffer() >= need) {
                flag(player, data, "FuftiACReach", "reach=" + String.format(java.util.Locale.US, "%.2f", reach)
                        + " max=" + String.format(java.util.Locale.US, "%.2f", max));
                data.setReachBuffer(need - 1);
            }
            if (event != null) {
                event.setCancelled(true);
            }
            return true;
        } else {
            data.setReachBuffer(Math.max(0, data.reachBuffer() - 1));
            return false;
        }
    }
}
