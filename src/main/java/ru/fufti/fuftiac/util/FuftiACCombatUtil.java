package ru.fufti.fuftiac.util;

import com.github.retrooper.packetevents.PacketEvents;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;

public final class FuftiACCombatUtil {

    private FuftiACCombatUtil() {
    }

    public static LivingEntity find(Player player, int entityId) {
        if (player.getEntityId() == entityId) {
            return player;
        }
        for (Player other : player.getWorld().getPlayers()) {
            if (other.getEntityId() == entityId) {
                return other;
            }
        }
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 8.0, 8.0, 8.0)) {
            if (entity.getEntityId() == entityId && entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    public static double reach(Player player, LivingEntity target) {
        Location eyeLoc = player.getEyeLocation();
        Vector eye = eyeLoc.toVector();
        BoundingBox box = target.getBoundingBox();

        double distCurrent = distanceToBox(eye, box);

        Vector targetVel = target.getVelocity();
        int ping = getPing(player);
        double ticks = Math.min(3.0, Math.max(1.0, ping / 50.0));

        double shiftX = -targetVel.getX() * ticks;
        double shiftY = -targetVel.getY() * ticks;
        double shiftZ = -targetVel.getZ() * ticks;

        BoundingBox pastBox = new BoundingBox(
                box.getMinX() + shiftX, box.getMinY() + shiftY, box.getMinZ() + shiftZ,
                box.getMaxX() + shiftX, box.getMaxY() + shiftY, box.getMaxZ() + shiftZ
        );
        double distPastTarget = distanceToBox(eye, pastBox);

        Vector playerVel = player.getVelocity();
        Vector pastEye = eye.clone().subtract(new Vector(playerVel.getX() * ticks, playerVel.getY() * ticks, playerVel.getZ() * ticks));

        double distPastAttacker = distanceToBox(pastEye, box);
        double distPastBoth = distanceToBox(pastEye, pastBox);

        return Math.min(Math.min(distCurrent, distPastTarget), Math.min(distPastAttacker, distPastBoth));
    }

    private static double distanceToBox(Vector point, BoundingBox box) {
        double clampedX = Math.max(box.getMinX(), Math.min(box.getMaxX(), point.getX()));
        double clampedY = Math.max(box.getMinY(), Math.min(box.getMaxY(), point.getY()));
        double clampedZ = Math.max(box.getMinZ(), Math.min(box.getMaxZ(), point.getZ()));

        double dx = point.getX() - clampedX;
        double dy = point.getY() - clampedY;
        double dz = point.getZ() - clampedZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double maxReach(Player player, FuftiAC plugin, String check) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return 4.5;
        }
        double base = plugin.fuftiConfig().decimal(check, "max-reach", 3.1);
        int ping = getPing(player);
        return base + Math.min(0.20, (ping / 1000.0) * 0.8);
    }

    public static double rayMissDistance(Player player, LivingEntity target) {
        Location eye = player.getEyeLocation();
        Vector origin = eye.toVector();
        Vector look = eye.getDirection().normalize();
        BoundingBox box = target.getBoundingBox();

        RayTraceResult hit = box.rayTrace(origin, look, 6.0);
        if (hit != null) {
            return 0.0;
        }

        double minMiss = rayBoxDistance(origin, look, box);

        Vector vel = target.getVelocity();
        if (vel.lengthSquared() > 0.001) {
            int ping = getPing(player);
            double ticks = Math.min(3.0, Math.max(1.0, ping / 50.0));
            double shiftX = -vel.getX() * ticks;
            double shiftY = -vel.getY() * ticks;
            double shiftZ = -vel.getZ() * ticks;
            BoundingBox pastBox = new BoundingBox(
                    box.getMinX() + shiftX, box.getMinY() + shiftY, box.getMinZ() + shiftZ,
                    box.getMaxX() + shiftX, box.getMaxY() + shiftY, box.getMaxZ() + shiftZ
            );
            double pastMiss = rayBoxDistance(origin, look, pastBox);
            if (pastMiss < minMiss) {
                minMiss = pastMiss;
            }
        }

        return minMiss;
    }

    public static double rayBoxDistance(Vector origin, Vector dir, BoundingBox box) {
        double low = 0.0;
        double high = 6.0;
        for (int i = 0; i < 18; i++) {
            double m1 = low + (high - low) / 3.0;
            double m2 = high - (high - low) / 3.0;
            double d1 = distanceToBoxPoint(origin, dir, m1, box);
            double d2 = distanceToBoxPoint(origin, dir, m2, box);
            if (d1 < d2) {
                high = m2;
            } else {
                low = m1;
            }
        }
        return distanceToBoxPoint(origin, dir, (low + high) * 0.5, box);
    }

    private static double distanceToBoxPoint(Vector origin, Vector dir, double t, BoundingBox box) {
        double px = origin.getX() + dir.getX() * t;
        double py = origin.getY() + dir.getY() * t;
        double pz = origin.getZ() + dir.getZ() * t;

        double dx = Math.max(0.0, Math.max(box.getMinX() - px, px - box.getMaxX()));
        double dy = Math.max(0.0, Math.max(box.getMinY() - py, py - box.getMaxY()));
        double dz = Math.max(0.0, Math.max(box.getMinZ() - pz, pz - box.getMaxZ()));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static boolean wallBetween(Player player, LivingEntity target) {
        Vector look = player.getEyeLocation().getDirection();
        if (look.lengthSquared() < 1.0E-6) {
            return false;
        }
        look.normalize();
        double max = Math.min(6.0, player.getEyeLocation().distance(target.getEyeLocation()) + 0.4);
        RayTraceResult blocks = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), look, max, FluidCollisionMode.NEVER, false
        );
        if (blocks == null || blocks.getHitBlock() == null) {
            return false;
        }
        Block block = blocks.getHitBlock();
        if (isPassableForCombat(block)) {
            return false;
        }
        RayTraceResult entity = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                look,
                max,
                0.15,
                candidate -> candidate.getUniqueId().equals(target.getUniqueId())
        );
        if (entity == null || entity.getHitEntity() == null) {
            return block.getType().isOccluding();
        }
        double blockDist = blocks.getHitPosition().distanceSquared(player.getEyeLocation().toVector());
        double entityDist = entity.getHitPosition().distanceSquared(player.getEyeLocation().toVector());
        return blockDist + 0.12 < entityDist;
    }

    public static boolean isPassableForCombat(Block block) {
        if (block == null) {
            return true;
        }
        Material mat = block.getType();
        if (mat.isAir() || !mat.isSolid()) {
            return true;
        }
        String name = mat.name();
        if (name.contains("GRASS")
                || name.contains("FERN")
                || name.contains("FLOWER")
                || name.contains("SAPLING")
                || name.contains("VINE")
                || name.contains("BUSH")
                || name.contains("LILY")
                || name.contains("CARPET")
                || name.contains("SNOW")
                || name.contains("BANNER")
                || name.contains("SIGN")
                || name.contains("TORCH")
                || name.contains("ROD")
                || name.contains("CHAIN")
                || name.contains("LANTERN")
                || name.contains("AMETHYST")
                || name.contains("DRIPSTONE")
                || name.contains("FROGSPAWN")
                || name.contains("CORAL")
                || name.contains("SEAGRASS")
                || name.contains("KELP")
                || name.contains("PALE_HANGING_MOSS")) {
            return true;
        }

        BlockData data = block.getBlockData();
        if (data instanceof Openable openable && openable.isOpen()) {
            return true;
        }

        if (name.contains("FENCE_GATE") && data instanceof Openable openableGate && openableGate.isOpen()) {
            return true;
        }

        if (name.contains("DOOR") && data instanceof Openable openableDoor && openableDoor.isOpen()) {
            return true;
        }

        return false;
    }

    public static void kick(Player player, Component reason) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            player.kick(reason);
            return;
        } catch (Throwable ignored) {
        }
        try {
            String legacy = LegacyComponentSerializer.legacySection().serialize(reason);
            player.kickPlayer(legacy);
        } catch (Throwable ignored) {
            player.kickPlayer("FuftiAC");
        }
    }

    public static void sendMessage(Player player, Component component) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (player instanceof Audience audience) {
            audience.sendMessage(component);
            return;
        }
        player.sendMessage(LegacyComponentSerializer.legacySection().serialize(component));
    }

    public static int getPing(Player player) {
        if (player == null) {
            return 0;
        }
        try {
            return player.getPing();
        } catch (Throwable ignored) {
        }
        try {
            return PacketEvents.getAPI().getPlayerManager().getPing(player);
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
