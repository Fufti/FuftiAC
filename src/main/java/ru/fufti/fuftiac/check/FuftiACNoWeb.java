package ru.fufti.fuftiac.check;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACNoWeb extends FuftiACCheck {

    public FuftiACNoWeb(FuftiAC plugin) {
        super(plugin, "NoWeb");
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled() || player.isFlying() || player.isGliding()) {
            data.setNoWebBuffer(0);
            return;
        }
        if (!inWeb(player)) {
            data.setNoWebBuffer(Math.max(0, data.noWebBuffer() - 1));
            return;
        }
        double max = this.plugin.fuftiConfig().decimal(name(), "max-speed", 0.09);
        if (data.speed() <= max) {
            data.setNoWebBuffer(Math.max(0, data.noWebBuffer() - 1));
            return;
        }
        data.setNoWebBuffer(data.noWebBuffer() + 1);
        int need = this.plugin.fuftiConfig().integer(name(), "buffer", 2);
        if (data.noWebBuffer() >= need) {
            flagMove(player, data, event, "FuftiACNoWeb", "speed=" + String.format(java.util.Locale.US, "%.3f", data.speed()));
            data.setNoWebBuffer(1);
        }
    }

    private static boolean inWeb(Player player) {
        return player.getLocation().getBlock().getType() == Material.COBWEB
                || player.getEyeLocation().getBlock().getType() == Material.COBWEB
                || player.getLocation().add(0.0, 0.4, 0.0).getBlock().getType() == Material.COBWEB;
    }
}
