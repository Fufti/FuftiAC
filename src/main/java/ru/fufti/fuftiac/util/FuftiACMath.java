package ru.fufti.fuftiac.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class FuftiACMath {

    private static final BlockFace[] HORIZONTAL = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private FuftiACMath() {
    }

    public static float[] rotationTo(Location eye, Location target) {
        double dx = target.getX() - eye.getX();
        double dy = target.getY() - eye.getY();
        double dz = target.getZ() - eye.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
        return new float[]{yaw, pitch};
    }

    public static double yawDiff(float a, float b) {
        return Math.abs(wrapAngle(a - b));
    }

    public static double pitchDiff(float a, float b) {
        return Math.abs(a - b);
    }

    public static double wrapAngle(double angle) {
        angle %= 360.0;
        if (angle >= 180.0) {
            angle -= 360.0;
        }
        if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }

    public static double angleTo(Location from, Location to) {
        Vector direction = from.getDirection();
        Vector target = to.toVector().subtract(from.toVector());
        if (target.lengthSquared() < 1.0E-6) {
            return 0.0;
        }
        return Math.toDegrees(direction.angle(target));
    }

    public static boolean againstWall(Player player) {
        Location loc = player.getLocation();
        for (BlockFace face : HORIZONTAL) {
            if (solid(loc.getBlock().getRelative(face)) || solid(loc.clone().add(0, 1, 0).getBlock().getRelative(face))) {
                return true;
            }
        }
        return false;
    }

    public static boolean collidingHorizontally(Player player) {
        var box = player.getBoundingBox().expand(0.12, 0.0, 0.12);
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.floor(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY());
        int maxY = (int) Math.floor(box.getMaxY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.floor(box.getMaxZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    if (!block.getType().isSolid() || isClimbable(block.getType())) {
                        continue;
                    }
                    if (block.getBoundingBox().overlaps(player.getBoundingBox().expand(0.05, -0.05, 0.05))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isClimbable(Material material) {
        return material == Material.LADDER
                || material == Material.VINE
                || material == Material.CAVE_VINES
                || material == Material.CAVE_VINES_PLANT
                || material == Material.TWISTING_VINES
                || material == Material.TWISTING_VINES_PLANT
                || material == Material.WEEPING_VINES
                || material == Material.WEEPING_VINES_PLANT
                || material == Material.SCAFFOLDING
                || material == Material.COBWEB
                || material == Material.POWDER_SNOW;
    }

    public static boolean solid(Block block) {
        return block.getType().isSolid() && !isClimbable(block.getType());
    }

    public static double stdDev(Iterable<Double> values) {
        int n = 0;
        double sum = 0.0;
        double sumSq = 0.0;
        for (double value : values) {
            n++;
            sum += value;
        }
        if (n < 2) {
            return 0.0;
        }
        double mean = sum / n;
        return Math.sqrt(Math.max(0.0, sumSq / n - mean * mean));
    }

    public static double distanceToBox(Location eye, BoundingBox box) {
        double x = clamp(eye.getX(), box.getMinX(), box.getMaxX());
        double y = clamp(eye.getY(), box.getMinY(), box.getMaxY());
        double z = clamp(eye.getZ(), box.getMinZ(), box.getMaxZ());
        return Math.sqrt(sq(eye.getX() - x) + sq(eye.getY() - y) + sq(eye.getZ() - z));
    }

    public static boolean rayHitsBox(Location eye, BoundingBox box, double maxDist) {
        Vector dir = eye.getDirection();
        if (dir.lengthSquared() < 1.0E-6) {
            return false;
        }
        dir.normalize();
        double tMin = 0.0;
        double tMax = maxDist;
        tMin = slab(eye.getX(), dir.getX(), box.getMinX(), box.getMaxX(), tMin, tMax);
        if (tMin < 0) {
            return false;
        }
        tMax = slabMax(eye.getX(), dir.getX(), box.getMinX(), box.getMaxX(), tMax);
        tMin = slab(eye.getY(), dir.getY(), box.getMinY(), box.getMaxY(), tMin, tMax);
        if (tMin < 0) {
            return false;
        }
        tMax = slabMax(eye.getY(), dir.getY(), box.getMinY(), box.getMaxY(), tMax);
        tMin = slab(eye.getZ(), dir.getZ(), box.getMinZ(), box.getMaxZ(), tMin, tMax);
        return tMin >= 0 && tMin <= tMax;
    }

    private static double slab(double origin, double dir, double min, double max, double tMin, double tMax) {
        if (Math.abs(dir) < 1.0E-8) {
            return origin >= min && origin <= max ? tMin : -1.0;
        }
        double t1 = (min - origin) / dir;
        double t2 = (max - origin) / dir;
        return Math.max(tMin, Math.min(t1, t2));
    }

    private static double slabMax(double origin, double dir, double min, double max, double tMax) {
        if (Math.abs(dir) < 1.0E-8) {
            return tMax;
        }
        double t1 = (min - origin) / dir;
        double t2 = (max - origin) / dir;
        return Math.min(tMax, Math.max(t1, t2));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double sq(double value) {
        return value * value;
    }

    public static double mean(Iterable<Double> values) {
        int n = 0;
        double sum = 0.0;
        for (double value : values) {
            n++;
            sum += value;
        }
        return n == 0 ? 0.0 : sum / n;
    }
}
