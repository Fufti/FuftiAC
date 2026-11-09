package ru.fufti.fuftiac.check;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;

public final class FuftiACNuker extends FuftiACCheck implements Listener {

    public FuftiACNuker(FuftiAC plugin) {
        super(plugin, "Nuker");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !enabled()) {
            data.setNukerBuffer(0);
            return;
        }

        Block block = event.getBlock();
        Location eye = player.getEyeLocation();
        double dist = eye.distance(block.getLocation().add(0.5, 0.5, 0.5));
        double maxDist = player.getGameMode() == GameMode.CREATIVE ? 6.0 : 5.4;

        if (dist > maxDist) {
            event.setCancelled(true);
            data.setNukerBuffer(data.nukerBuffer() + 1);
            flag(player, data, "FuftiACNuker", "reach distance=" + String.format(java.util.Locale.US, "%.2f", dist));
            return;
        }

        if (isNotFacingBlock(player, block)) {
            event.setCancelled(true);
            data.setNukerBuffer(data.nukerBuffer() + 1);
            flag(player, data, "FuftiACNuker", "broken block without aiming at hitbox");
            return;
        }

        data.addBlockBreak();

        int in60ms = data.breaksInWindow(60L);
        int in250ms = data.breaksInWindow(250L);
        int in1000ms = data.breaksInWindow(1000L);

        boolean nuke = in60ms >= 2 || in250ms >= 5 || in1000ms >= 18;
        if (nuke) {
            data.setNukerBuffer(data.nukerBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 1);
            if (data.nukerBuffer() >= need) {
                flag(player, data, "FuftiACNuker", "multi-break 60ms=" + in60ms
                        + " 250ms=" + in250ms + " 1s=" + in1000ms);
                event.setCancelled(true);
                data.setNukerBuffer(need - 1);
            }
        } else {
            data.setNukerBuffer(Math.max(0, data.nukerBuffer() - 1));
        }
    }

    private static boolean isNotFacingBlock(Player player, Block block) {
        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();
        BoundingBox box = block.getBoundingBox();

        Vector eyePos = eye.toVector();
        RayTraceResult hit = box.rayTrace(eyePos, look, 6.0);
        if (hit != null) {
            return false;
        }

        BoundingBox expanded = box.clone().expand(0.6, 0.6, 0.6);
        RayTraceResult expHit = expanded.rayTrace(eyePos, look, 6.0);
        if (expHit != null) {
            return false;
        }

        Vector blockCenter = block.getLocation().add(0.5, 0.5, 0.5).toVector();
        Vector toCenter = blockCenter.clone().subtract(eyePos);
        if (toCenter.lengthSquared() < 0.001) {
            return false;
        }

        double dot = look.dot(toCenter.clone().normalize());
        double angle = Math.toDegrees(look.angle(toCenter));
        if (angle < 95.0 || dot > 0.0) {
            return false;
        }

        double missDist = FuftiACCombatUtil.rayBoxDistance(eyePos, look, box);
        return missDist > 1.2 && angle > 110.0;
    }
}
