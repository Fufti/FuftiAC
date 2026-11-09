package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACBoatFly extends FuftiACCheck implements Listener {

    public FuftiACBoatFly(FuftiAC plugin) {
        super(plugin, "BoatFly");
    }

    @Override
    public boolean exempt(Player player, FuftiACPlayerData data) {
        return data.punishing()
                || player.isDead()
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        Entity vehicle = event.getVehicle();
        if (!(vehicle instanceof Boat boat)) {
            return;
        }
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof Player player) {
                checkBoat(player, boat, event.getTo(), event.getFrom());
            }
        }
    }

    public void checkBoat(Player player, Boat boat, Location to, Location from) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !enabled()) {
            data.setBoatFlyBuffer(0);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastDamageMs() < 600L || data.hasPendingVelocity() || now - data.lastVelocityMs() < 600L) {
            data.setBoatFlyBuffer(0);
            return;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dy = to.getY() - from.getY();
        double horizSpeed = Math.hypot(dx, dz);

        if (isOnIceOrWater(to)) {
            data.setBoatFlyBuffer(Math.max(0, data.boatFlyBuffer() - 1));
            return;
        }

        boolean onSolidGround = isOnSolidGround(to);
        boolean illegal = false;
        String reason = "";

        float boatYaw = boat.getLocation().getYaw();
        double rad = Math.toRadians(boatYaw);
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);

        if (horizSpeed > 0.06) {
            double dot = (dx * forwardX + dz * forwardZ) / horizSpeed;
            if (dot < -0.15) {
                illegal = true;
                reason = "backward boat movement speed=" + round(horizSpeed) + " dot=" + round(dot);
            } else if (Math.abs(dot) < 0.20) {
                illegal = true;
                reason = "sideways boat movement speed=" + round(horizSpeed) + " dot=" + round(dot);
            }
        }

        if (!illegal) {
            if (onSolidGround) {
                if (horizSpeed > 0.32) {
                    illegal = true;
                    reason = "land speed=" + round(horizSpeed);
                }
            } else {
                boolean ascending = dy > 0.01;
                boolean hovering = Math.abs(dy) < 0.05 && horizSpeed > 0.03;
                boolean horizontalAirFlight = horizSpeed > 0.04;
                boolean slowFallFlight = dy > -0.15 && horizSpeed > 0.03;

                if (ascending || hovering || horizontalAirFlight || slowFallFlight) {
                    illegal = true;
                    reason = "air flight speed=" + round(horizSpeed) + " dy=" + round(dy);
                }
            }
        }

        if (illegal) {
            data.setBoatFlyBuffer(data.boatFlyBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 1);
            if (data.boatFlyBuffer() >= need) {
                flag(player, data, "FuftiACBoatFly", reason);
                boat.eject();
                boat.remove();
                Location safe = from.clone();
                safe.setY(safe.getY() + 0.1);
                player.teleport(safe);
                player.setVelocity(new Vector(0, -0.08, 0));
                data.setBoatFlyBuffer(0);
            }
        } else {
            data.setBoatFlyBuffer(Math.max(0, data.boatFlyBuffer() - 1));
        }
    }

    private static boolean isOnIceOrWater(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        for (double x = -0.5; x <= 0.5; x += 0.5) {
            for (double z = -0.5; z <= 0.5; z += 0.5) {
                for (double y = -0.3; y <= 0.1; y += 0.2) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    String name = mat.name();
                    if (name.contains("WATER") || name.contains("LAVA") || name.contains("ICE")
                            || name.contains("BUBBLE") || name.contains("SLIME") || name.contains("LILY")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isOnSolidGround(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        for (double x = -0.4; x <= 0.4; x += 0.4) {
            for (double z = -0.4; z <= 0.4; z += 0.4) {
                for (double y = -0.35; y <= 0.05; y += 0.20) {
                    Block b = loc.clone().add(x, y, z).getBlock();
                    Material mat = b.getType();
                    if (mat.isSolid()) {
                        return true;
                    }
                    String name = mat.name();
                    if (name.contains("SLAB") || name.contains("STAIR") || name.contains("CARPET")
                            || name.contains("PATH") || name.contains("FARMLAND") || name.contains("SNOW")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String round(double value) {
        return String.format(java.util.Locale.US, "%.3f", value);
    }
}
