package ru.fufti.fuftiac.check;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACMoveListener implements Listener {

    private final FuftiAC plugin;

    public FuftiACMoveListener(FuftiAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        Player player = event.getPlayer();
        FuftiACPlayerData data = this.plugin.data().get(player);
        data.updateMove(event.getTo(), event.getFrom());
        if (data.punishing()) {
            return;
        }
        this.plugin.checks().targetStrafe().checkMove(player, data, event.getFrom(), event.getTo());
        this.plugin.checks().spider().checkMove(player, data, event);
        this.plugin.checks().noWeb().checkMove(player, data, event);
        this.plugin.checks().noSlow().checkMove(player, data, event);
        this.plugin.checks().speed().checkMove(player, data, event);
        this.plugin.checks().elytraSwap().checkMove(player, data);
        this.plugin.checks().fly().checkMove(player, data, event);
        this.plugin.checks().jesus().checkMove(player, data, event);
        this.plugin.checks().noFall().checkMove(player, data, event);
        this.plugin.checks().step().checkMove(player, data, event);
        this.plugin.checks().airJump().checkMove(player, data, event);
        this.plugin.checks().airStuck().checkMove(player, data, event.getFrom(), event.getTo(), event);
        this.plugin.checks().noClip().checkMove(player, event.getTo(), event.getFrom(), data, event);
        this.plugin.checks().elytraRecast().checkMove(player, data, event);
    }
}
