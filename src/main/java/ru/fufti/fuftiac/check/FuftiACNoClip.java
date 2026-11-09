package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACNoClip extends FuftiACCheck {

    public FuftiACNoClip(FuftiAC plugin) {
        super(plugin, "NoClip");
    }

    public void checkMove(Player player, Location to, Location from, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || !enabled()) {
            data.setNoClipBuffer(0);
            return;
        }

        if (player.isGliding() || player.isSwimming() || player.isRiptiding()) {
            data.setNoClipBuffer(0);
            return;
        }

        double speed = data.speed();
        if (speed < 0.08) {
            data.setNoClipBuffer(Math.max(0, data.noClipBuffer() - 1));
            return;
        }

        Block torso = to.clone().add(0, 0.7, 0).getBlock();
        Block feet = to.getBlock();

        if (isOccludingSolid(torso) && isOccludingSolid(feet)) {
            data.setNoClipBuffer(data.noClipBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 1);
            if (data.noClipBuffer() >= need) {
                flagMove(player, data, event, "FuftiACNoClip", "inside " + torso.getType().name()
                        + " speed=" + String.format(java.util.Locale.US, "%.2f", speed));
                data.setNoClipBuffer(need - 1);
            }
        } else {
            data.setNoClipBuffer(Math.max(0, data.noClipBuffer() - 1));
        }
    }

    private static boolean isOccludingSolid(Block block) {
        Material mat = block.getType();
        if (!mat.isSolid() || !mat.isOccluding()) {
            return false;
        }
        String name = mat.name();
        return !name.contains("LEAVES") && !name.contains("GLASS") && !name.contains("SHULKER")
                && !name.contains("PISTON") && !name.contains("BARREL") && !name.contains("CHEST")
                && !name.contains("DOOR") && !name.contains("GATE") && !name.contains("BED");
    }
}
