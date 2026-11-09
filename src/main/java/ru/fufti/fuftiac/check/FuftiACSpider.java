package ru.fufti.fuftiac.check;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACMath;
import ru.fufti.fuftiac.util.FuftiACScheduler;

public final class FuftiACSpider extends FuftiACCheck implements Listener {

    public FuftiACSpider(FuftiAC plugin) {
        super(plugin, "Spider");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        boolean cancelled = event.isCancelled();
        Location loc = block.getLocation().clone();
        FuftiACScheduler.runEntityTaskLater(this.plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            Material type = loc.getBlock().getType();
            boolean gone = !type.isSolid() || type.isAir();
            if (cancelled || gone) {
                this.plugin.data().get(player).markGhostPlace(loc);
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBucket(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        boolean cancelled = event.isCancelled();
        Location loc = block.getLocation().clone();
        FuftiACScheduler.runEntityTaskLater(this.plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            Material type = loc.getBlock().getType();
            boolean waterGone = type != Material.WATER && type != Material.LAVA && !type.name().contains("BUBBLE");
            if (cancelled || waterGone) {
                this.plugin.data().get(player).markGhostPlace(loc);
            }
        }, 1L);
    }

    public void checkMove(Player player, FuftiACPlayerData data, PlayerMoveEvent event) {
        if (exempt(player, data) || player.isFlying() || player.isGliding() || player.isClimbing()) {
            data.setSpiderBuffer(0);
            return;
        }
        if (player.hasPotionEffect(PotionEffectType.JUMP_BOOST) || player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            data.setSpiderBuffer(0);
            return;
        }
        if (player.getLocation().getBlock().isLiquid() || player.getEyeLocation().getBlock().isLiquid()) {
            data.setSpiderBuffer(0);
            return;
        }
        double deltaY = data.deltaY();
        double minY = this.plugin.fuftiConfig().decimal(name(), "min-y", 0.12);
        if (deltaY <= minY) {
            data.setSpiderBuffer(Math.max(0, data.spiderBuffer() - 1));
            return;
        }
        if (data.onGround()) {
            data.setSpiderBuffer(Math.max(0, data.spiderBuffer() - 1));
            return;
        }
        if (!FuftiACMath.collidingHorizontally(player)) {
            data.setSpiderBuffer(Math.max(0, data.spiderBuffer() - 1));
            return;
        }

        long ghostMs = this.plugin.fuftiConfig().millis(name(), "ghost-ms", 600L);
        boolean ghostNear = data.lastGhostPlace() != null
                && System.currentTimeMillis() - data.lastGhostPlaceMs() < ghostMs
                && data.lastGhostPlace().getWorld().equals(player.getWorld())
                && data.lastGhostPlace().distanceSquared(player.getLocation()) < 4.0;
        if (ghostNear) {
            data.setSpiderBuffer(data.spiderBuffer() + 2);
            if (data.spiderBuffer() >= 3) {
                flagMove(player, data, event, "FuftiACSpider", "dy=" + round(deltaY) + " ghost-block-climb");
                data.setSpiderBuffer(0);
            }
            return;
        }

        data.setSpiderBuffer(data.spiderBuffer() + 1);
        int max = this.plugin.fuftiConfig().integer(name(), "buffer", 3);
        if (data.spiderBuffer() >= max) {
            flagMove(player, data, event, "FuftiACSpider", "dy=" + round(deltaY) + " wall-climb");
            data.setSpiderBuffer(1);
        }
    }

    private static String round(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
