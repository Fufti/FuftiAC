package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;
import ru.fufti.fuftiac.util.FuftiACScheduler;

public final class FuftiACVelocity extends FuftiACCheck implements Listener {

    public FuftiACVelocity(FuftiAC plugin) {
        super(plugin, "Velocity");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (data.setbackPushActive()) {
            data.setSetbackPushActive(false);
            return;
        }
        if (exempt(player, data) || !enabled()) {
            return;
        }
        Vector vel = event.getVelocity();
        double horiz = Math.hypot(vel.getX(), vel.getZ());
        if (horiz < 0.18) {
            return;
        }

        Location startLoc = player.getLocation().clone();
        data.setPendingVelocity(vel, startLoc);

        FuftiACScheduler.runEntityTaskLater(this.plugin, player, () -> {
            if (!player.isOnline() || player.isDead()) {
                data.clearPendingVelocity();
                return;
            }
            verifyVelocity(player, data, vel, startLoc, horiz);
        }, 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        FuftiACPlayerData data = this.plugin.data().get(victim);
        if (exempt(victim, data) || !enabled()) {
            return;
        }
        if (victim.isBlocking()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastVelocityCheckMs() < 500L) {
            return;
        }
        data.setLastVelocityCheckMs(now);

        Entity damager = event.getDamager();
        Entity attackerEntity = damager;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            attackerEntity = shooter;
        }
        if (attackerEntity == null || attackerEntity.equals(victim)) {
            return;
        }

        Vector dir = victim.getLocation().toVector().subtract(attackerEntity.getLocation().toVector()).setY(0);
        if (dir.lengthSquared() < 0.0001) {
            dir = attackerEntity.getLocation().getDirection().setY(0);
        }
        if (dir.lengthSquared() < 0.0001) {
            dir = new Vector(0, 0, 1);
        }
        dir.normalize();

        double kbStrength = 0.38;
        if (attackerEntity instanceof Player pAttacker) {
            if (pAttacker.isSprinting()) {
                kbStrength += 0.35;
            }
            ItemStack weapon = pAttacker.getInventory().getItemInMainHand();
            if (weapon != null && weapon.hasItemMeta()) {
                try {
                    int kbLevel = weapon.getEnchantmentLevel(Enchantment.KNOCKBACK);
                    if (kbLevel > 0) {
                        kbStrength += (kbLevel * 0.40);
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        try {
            Attribute kbAttr = null;
            try {
                kbAttr = Attribute.KNOCKBACK_RESISTANCE;
            } catch (Throwable ignored) {
            }
            if (kbAttr == null) {
                try {
                    kbAttr = Attribute.valueOf("GENERIC_KNOCKBACK_RESISTANCE");
                } catch (Throwable ignored) {
                }
            }
            if (kbAttr != null) {
                AttributeInstance attr = victim.getAttribute(kbAttr);
                if (attr != null) {
                    double res = attr.getValue();
                    if (res >= 0.85) {
                        return;
                    }
                    kbStrength *= (1.0 - res);
                }
            }
        } catch (Throwable ignored) {
        }

        Vector expectedVel = dir.clone().multiply(kbStrength);
        expectedVel.setY(0.35);
        Location startLoc = victim.getLocation().clone();
        data.setPendingVelocity(expectedVel, startLoc);

        final double finalExpHoriz = kbStrength;
        int victimPing = FuftiACCombatUtil.getPing(victim);
        long delayTicks = Math.max(5L, Math.min(10L, (victimPing / 50L) + 3L));
        FuftiACScheduler.runEntityTaskLater(this.plugin, victim, () -> {
            if (!victim.isOnline() || victim.isDead()) {
                data.clearPendingVelocity();
                return;
            }
            verifyVelocity(victim, data, expectedVel, startLoc, finalExpHoriz);
        }, delayTicks);
    }

    private void verifyVelocity(Player player, FuftiACPlayerData data, Vector expected, Location startLoc, double expHoriz) {
        data.clearPendingVelocity();
        if (exempt(player, data) || !enabled()) {
            data.setVelocityBuffer(0);
            return;
        }

        if (hasObstructionInDir(startLoc, player.getLocation(), expected)) {
            data.setVelocityBuffer(Math.max(0, data.velocityBuffer() - 1));
            return;
        }

        Vector dir = expected.clone().setY(0).normalize();
        Vector moved = player.getLocation().toVector().subtract(startLoc.toVector());
        double forwardMovement = moved.dot(dir);
        double totalHorizMovement = Math.hypot(moved.getX(), moved.getZ());

        if (forwardMovement < 0.04 && totalHorizMovement < 0.07) {
            data.setVelocityBuffer(data.velocityBuffer() + 1);
            int need = this.plugin.fuftiConfig().integer(name(), "buffer", 3);
            if (data.velocityBuffer() >= need) {
                flag(player, data, "FuftiACVelocity", "exp=" + String.format(java.util.Locale.US, "%.2f", expHoriz)
                        + " act=" + String.format(java.util.Locale.US, "%.2f", forwardMovement));
                player.teleport(startLoc);
                Vector push = expected.clone();
                if (push.getY() <= 0.05) {
                    push.setY(0.32);
                }
                data.setSetbackPushActive(true);
                player.setVelocity(push);
                data.setVelocityBuffer(0);
            }
        } else {
            data.setVelocityBuffer(Math.max(0, data.velocityBuffer() - 1));
            this.plugin.checks().velocityLegit().checkVelocity(player, data, expected, Math.max(forwardMovement, totalHorizMovement));
        }
    }

    public static boolean hasObstructionInDir(Location startLoc, Location currentLoc, Vector velocity) {
        if (startLoc == null || currentLoc == null || velocity == null) {
            return false;
        }
        Block feet = currentLoc.getBlock();
        Material feetMat = feet.getType();
        if (feetMat == Material.WATER || feetMat == Material.LAVA || feetMat.name().contains("WEB")
                || feetMat.name().contains("COBWEB") || feetMat.name().contains("CLIMBABLE")
                || feetMat.name().contains("LADDER") || feetMat.name().contains("VINE")) {
            return true;
        }

        Vector dir = velocity.clone().setY(0);
        if (dir.lengthSquared() < 0.001) {
            return false;
        }
        dir.normalize();

        for (double d = 0.45; d <= 1.2; d += 0.35) {
            for (double y = 0.4; y <= 1.4; y += 0.5) {
                Block b1 = startLoc.clone().add(dir.getX() * d, y, dir.getZ() * d).getBlock();
                if (b1.getType().isSolid() && b1.getType().isOccluding()) {
                    return true;
                }
                Block b2 = currentLoc.clone().add(dir.getX() * d, y, dir.getZ() * d).getBlock();
                if (b2.getType().isSolid() && b2.getType().isOccluding()) {
                    return true;
                }
            }
        }
        return false;
    }
}
