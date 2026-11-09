package ru.fufti.fuftiac.check;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FuftiACScaffold extends FuftiACCheck implements Listener {

    private final Map<UUID, Long> lastPlaceTime = new HashMap<>();
    private final Map<UUID, Integer> rapidPlaces = new HashMap<>();

    public FuftiACScaffold(FuftiAC plugin) {
        super(plugin, "Scaffold");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !enabled() || player.isFlying() || player.isGliding()) {
            return;
        }

        Block placed = event.getBlockPlaced();
        Block against = event.getBlockAgainst();
        Location playerLoc = player.getLocation();
        double py = playerLoc.getY();
        double by = placed.getY();

        if (by > py + 0.5) {
            data.setScaffoldBuffer(Math.max(0, data.scaffoldBuffer() - 1));
            return;
        }

        double speed = data.speed();
        boolean moving = player.isSprinting() || speed > 0.08;
        BlockFace face = against.getFace(placed);
        boolean placingUnderOrSide = face != null && (face == BlockFace.DOWN || by <= py);

        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();

        BoundingBox againstBox = against.getBoundingBox();
        RayTraceResult hitAgainst = againstBox.rayTrace(eye.toVector(), look, 5.0);
        float pitch = playerLoc.getPitch();

        if (hitAgainst == null && pitch < 65.0f && by <= py) {
            data.setScaffoldBuffer(data.scaffoldBuffer() + 2);
            if (data.scaffoldBuffer() >= 2) {
                flag(player, data, "FuftiACScaffold Forward", "bridging without aiming at target block pitch=" + round(pitch));
                event.setCancelled(true);
                data.setScaffoldBuffer(1);
            }
            return;
        }

        if (face != null && face != BlockFace.UP && face != BlockFace.DOWN && by <= py) {
            Vector faceNormal = new Vector(face.getModX(), face.getModY(), face.getModZ());
            Vector faceCenter = against.getLocation().add(0.5, 0.5, 0.5).toVector()
                    .add(faceNormal.clone().multiply(0.5));
            Vector toEye = eye.toVector().subtract(faceCenter);
            double facingDot = toEye.dot(faceNormal);

            if (facingDot < -0.05) {
                data.setScaffoldBuffer(data.scaffoldBuffer() + 2);
                if (data.scaffoldBuffer() >= 2) {
                    flag(player, data, "FuftiACScaffold Backface", "clicked hidden block face facingDot=" + round(facingDot));
                    event.setCancelled(true);
                    data.setScaffoldBuffer(1);
                }
                return;
            }
        }

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();
        long prev = this.lastPlaceTime.getOrDefault(uuid, 0L);
        long diff = now - prev;
        this.lastPlaceTime.put(uuid, now);

        if (moving && placingUnderOrSide && !player.isSneaking()) {
            if (diff < 120L) {
                int count = this.rapidPlaces.getOrDefault(uuid, 0) + 1;
                this.rapidPlaces.put(uuid, count);
                if (count >= 2) {
                    data.setScaffoldBuffer(data.scaffoldBuffer() + 2);
                    if (data.scaffoldBuffer() >= 2) {
                        flag(player, data, "FuftiACScaffold Fast", "rapid block placement diff=" + diff + "ms speed=" + round(speed));
                        event.setCancelled(true);
                        data.setScaffoldBuffer(1);
                    }
                    return;
                }
            } else {
                this.rapidPlaces.put(uuid, 0);
            }

            Vector motion = new Vector(playerLoc.getX() - data.lastX(), 0, playerLoc.getZ() - data.lastZ());
            if (motion.lengthSquared() > 0.002) {
                motion.normalize();
                Vector horizLook = new Vector(look.getX(), 0, look.getZ());
                if (horizLook.lengthSquared() > 0.001) {
                    horizLook.normalize();
                    double motionLookDot = motion.dot(horizLook);

                    if (motionLookDot > 0.55 && pitch < 68.0f) {
                        data.setScaffoldBuffer(data.scaffoldBuffer() + 2);
                        if (data.scaffoldBuffer() >= 2) {
                            flag(player, data, "FuftiACScaffold Forward", "bridging forward without looking down pitch=" + round(pitch));
                            event.setCancelled(true);
                            data.setScaffoldBuffer(1);
                        }
                        return;
                    }
                }
            }
        } else {
            this.rapidPlaces.put(uuid, 0);
        }

        Vector horizToBlock = new Vector(placed.getX() + 0.5 - eye.getX(), 0, placed.getZ() + 0.5 - eye.getZ());
        if (horizToBlock.length() > 3.4 && moving && by <= py) {
            data.setScaffoldBuffer(data.scaffoldBuffer() + 2);
            if (data.scaffoldBuffer() >= 2) {
                flag(player, data, "FuftiACScaffold Expand", "placed block too far dist=" + round(horizToBlock.length()));
                event.setCancelled(true);
                data.setScaffoldBuffer(1);
            }
            return;
        }

        data.setScaffoldBuffer(Math.max(0, data.scaffoldBuffer() - 1));
    }

    private static String round(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
