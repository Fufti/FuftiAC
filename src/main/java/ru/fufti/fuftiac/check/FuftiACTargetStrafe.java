package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACMath;

public final class FuftiACTargetStrafe extends FuftiACCheck {

    public FuftiACTargetStrafe(FuftiAC plugin) {
        super(plugin, "TargetStrafe");
    }

    public void checkMove(Player player, FuftiACPlayerData data, Location from, Location to) {
        if (exempt(player, data)) {
            data.setCircleTicks(0);
            return;
        }
        long combatMs = this.plugin.fuftiConfig().millis(name(), "combat-ms", 2500L);
        if (System.currentTimeMillis() - data.lastAttackMs() > combatMs) {
            data.setCircleTicks(0);
            return;
        }
        Player target = findTarget(player, data);
        if (target == null) {
            data.setCircleTicks(0);
            return;
        }

        double speed = data.speed();
        if (speed < this.plugin.fuftiConfig().decimal(name(), "min-speed", 0.18)) {
            data.setCircleTicks(0);
            return;
        }

        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector());
        toTarget.setY(0);
        if (toTarget.lengthSquared() < 0.04 || toTarget.lengthSquared() > 36.0) {
            data.setCircleTicks(0);
            return;
        }
        Vector motion = new Vector(to.getX() - from.getX(), 0, to.getZ() - from.getZ());
        if (motion.lengthSquared() < 1.0E-4) {
            return;
        }
        toTarget.normalize();
        motion.normalize();
        double dot = motion.dot(toTarget);
        double cross = motion.getX() * toTarget.getZ() - motion.getZ() * toTarget.getX();
        int sign = cross >= 0 ? 1 : -1;
        double angle = Math.atan2(toTarget.getZ(), toTarget.getX());

        boolean circling = Math.abs(dot) < 0.35 && Math.abs(cross) > 0.55;
        boolean sameDir = data.lastCircleSign() == 0 || data.lastCircleSign() == sign;
        boolean radiusStable = Math.abs(FuftiACMath.wrapAngle(Math.toDegrees(angle) - data.lastTargetAngle())) > 4.0;

        if (circling && sameDir && radiusStable) {
            data.setCircleTicks(data.circleTicks() + 1);
        } else {
            data.setCircleTicks(Math.max(0, data.circleTicks() - 2));
        }
        data.setLastCircleSign(sign);
        data.setLastTargetAngle(Math.toDegrees(angle));

        int need = this.plugin.fuftiConfig().integer(name(), "circle-ticks", 12);
        if (data.circleTicks() >= need && FuftiACMath.collidingHorizontally(player)) {
            flag(player, data, "FuftiACTargetStrafe Collision", "ticks=" + data.circleTicks() + " target=" + target.getName());
            data.setCircleTicks(need / 2);
        }
    }

    private Player findTarget(Player player, FuftiACPlayerData data) {
        if (data.lastAttackTarget() != null) {
            Player stored = this.plugin.getServer().getPlayer(data.lastAttackTarget());
            if (stored != null && stored.getWorld().equals(player.getWorld())
                    && stored.getLocation().distanceSquared(player.getLocation()) <= 36.0) {
                return stored;
            }
        }
        Player best = null;
        double bestDist = 36.0;
        for (Player other : player.getWorld().getPlayers()) {
            if (other.equals(player)) {
                continue;
            }
            double dist = other.getLocation().distanceSquared(player.getLocation());
            if (dist < bestDist) {
                bestDist = dist;
                best = other;
            }
        }
        return best;
    }
}
