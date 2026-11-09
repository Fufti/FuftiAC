package ru.fufti.fuftiac.check;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public abstract class FuftiACCheck {

    protected final FuftiAC plugin;
    private final String name;

    protected FuftiACCheck(FuftiAC plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String name() {
        return this.name;
    }

    public boolean enabled() {
        return this.plugin.fuftiConfig().checkEnabled(this.name);
    }

    public int maxVl() {
        return this.plugin.fuftiConfig().maxVl(this.name);
    }

    public boolean exempt(Player player, FuftiACPlayerData data) {
        return data.punishing()
                || player.isDead()
                || player.isInsideVehicle()
                || player.getGameMode() == GameMode.SPECTATOR;
    }

    public void flag(Player player, FuftiACPlayerData data, String alertName, String detail) {
        if (!enabled() || exempt(player, data)) {
            return;
        }
        int vl = data.addVl(this.name);
        int max = maxVl();
        this.plugin.alerts().flag(player, alertName, vl, max, detail);
        if (vl >= max && this.plugin.fuftiConfig().punishmentsEnabled()) {
            this.plugin.punish().punish(player, this.name);
        }
    }

    public void flag(Player player, FuftiACPlayerData data, String detail) {
        flag(player, data, this.name, detail);
    }

    public void flagMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event, String alertName, String detail) {
        flag(player, data, alertName, detail);
        setback(player, data, event);
    }

    public void setback(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        Location valid = data.lastValidLocation();
        Location from = event != null ? event.getFrom() : null;
        Location target = valid;
        if (target == null || (from != null && (!target.getWorld().equals(from.getWorld()) || target.distanceSquared(from) > 144.0))) {
            target = from;
        }
        if (target != null && player.isOnline()) {
            Location snap = target.clone();
            snap.setYaw(player.getLocation().getYaw());
            snap.setPitch(player.getLocation().getPitch());
            if (event != null) {
                event.setTo(snap);
            }
            player.teleport(snap);
        } else if (event != null && player.isOnline()) {
            event.setTo(event.getFrom());
            player.teleport(event.getFrom());
        }
        player.setVelocity(new Vector(0, -0.15, 0));
    }
}
