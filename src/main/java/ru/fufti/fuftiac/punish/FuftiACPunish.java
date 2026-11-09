package ru.fufti.fuftiac.punish;

import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;
import ru.fufti.fuftiac.util.FuftiACScheduler;
import ru.fufti.fuftiac.util.FuftiACTask;
import ru.fufti.fuftiac.util.FuftiACTime;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public final class FuftiACPunish {

    private final FuftiAC plugin;

    public FuftiACPunish(FuftiAC plugin) {
        this.plugin = plugin;
    }

    public void punish(Player player, String check) {
        if (!this.plugin.fuftiConfig().punishmentsEnabled()) {
            return;
        }
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (data.punishing()) {
            return;
        }
        String command = this.plugin.fuftiConfig().punishCommand(check)
                .replace("%player%", player.getName())
                .replace("%check%", check)
                .replace("%vl%", String.valueOf(data.vl(check)))
                .replace("%max%", String.valueOf(this.plugin.fuftiConfig().maxVl(check)))
                .replace("%reason%", check);
        runAnimated(player, () -> dispatch(command, check, player.getName()));
    }

    public void ban(CommandSender sender, Player target, Duration duration, String reason) {
        String label = FuftiACTime.pretty(duration);
        runAnimated(target, () -> applyBan(target.getName(), duration, reason, true));
        Component msg = this.plugin.alerts().color(
                this.plugin.fuftiConfig().message("banned")
                        .replace("%player%", target.getName())
                        .replace("%duration%", label)
                        .replace("%reason%", reason)
        );
        if (sender instanceof Player staff) {
            FuftiACCombatUtil.sendMessage(staff, msg);
        } else {
            sender.sendMessage(this.plugin.alerts().color(this.plugin.fuftiConfig().message("banned")
                    .replace("%player%", target.getName())
                    .replace("%duration%", label)
                    .replace("%reason%", reason)).toString());
        }
    }

    public void unban(CommandSender sender, String name) {
        Bukkit.getBanList(BanList.Type.NAME).pardon(name);
        Component msg = this.plugin.alerts().color(
                this.plugin.fuftiConfig().message("unbanned").replace("%player%", name)
        );
        if (sender instanceof Player staff) {
            FuftiACCombatUtil.sendMessage(staff, msg);
        } else {
            sender.sendMessage(name + " unbanned");
        }
    }

    public void banOffline(CommandSender sender, String name, Duration duration, String reason) {
        applyBan(name, duration, reason, false);
        Component msg = this.plugin.alerts().color(
                this.plugin.fuftiConfig().message("banned")
                        .replace("%player%", name)
                        .replace("%duration%", FuftiACTime.pretty(duration))
                        .replace("%reason%", reason)
        );
        if (sender instanceof Player staff) {
            FuftiACCombatUtil.sendMessage(staff, msg);
        } else {
            sender.sendMessage(name + " banned offline");
        }
    }

    public void resetMovement(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (data != null) {
            data.setAnimationTask(null);
            data.setPunishing(false);
        }
        player.setGravity(true);
        player.setVelocity(new Vector(0, -0.08, 0));
        player.setFallDistance(0.0f);
        player.removePotionEffect(PotionEffectType.LEVITATION);
        PotionEffectType dark = getDarknessEffect();
        if (dark != null) {
            player.removePotionEffect(dark);
        }
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        boolean canFly = player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
        player.setFlying(false);
        player.setAllowFlight(canFly);
    }

    private void runAnimated(Player player, Runnable finish) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (data.punishing()) {
            return;
        }
        data.setPunishing(true);
        if (!this.plugin.fuftiConfig().animationEnabled() || !player.isOnline()) {
            resetMovement(player);
            data.setPunishing(false);
            finish.run();
            return;
        }

        player.closeInventory();
        player.setGravity(false);
        player.setVelocity(new Vector(0, this.plugin.fuftiConfig().riseSpeed(), 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 200, 2, false, false, false));
        PotionEffectType dark = getDarknessEffect();
        if (dark != null) {
            player.addPotionEffect(new PotionEffect(dark, 200, 0, false, false, false));
        }

        int ticks = Math.max(10, this.plugin.fuftiConfig().animationTicks());
        double rise = this.plugin.fuftiConfig().riseSpeed();
        boolean sound = this.plugin.fuftiConfig().animationSound();
        boolean particles = this.plugin.fuftiConfig().animationParticles();
        AtomicInteger tick = new AtomicInteger();

        FuftiACTask[] holder = new FuftiACTask[1];
        holder[0] = FuftiACScheduler.runEntityTaskTimer(this.plugin, player, () -> {
            int current = tick.incrementAndGet();
            if (!player.isOnline() || current >= ticks) {
                resetMovement(player);
                if (player.isOnline() && sound) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.35f, 1.8f);
                }
                if (holder[0] != null) {
                    holder[0].cancel();
                }
                data.setAnimationTask(null);
                finish.run();
                return;
            }
            Location loc = player.getLocation().clone();
            loc.setY(loc.getY() + rise);
            player.teleport(loc);
            player.setVelocity(new Vector(0, rise, 0));
            if (particles) {
                player.getWorld().spawnParticle(Particle.CLOUD, loc, 6, 0.22, 0.3, 0.22, 0.02);
                player.getWorld().spawnParticle(getSmokeParticle(), loc, 4, 0.18, 0.32, 0.18, 0.01);
            }
            if (sound && current % 5 == 0) {
                player.getWorld().playSound(loc, Sound.ENTITY_PHANTOM_FLAP, 0.55f, 1.45f);
            }
        }, 1L, 1L);
        data.setAnimationTask(holder[0]);
    }

    private void applyBan(String name, Duration duration, String reason, boolean kick) {
        Date expires = duration == null || duration.isZero() ? null : new Date(System.currentTimeMillis() + duration.toMillis());
        Bukkit.getBanList(BanList.Type.NAME).addBan(name, reason, expires, "FuftiAC");
        Player online = Bukkit.getPlayerExact(name);
        if (kick && online != null) {
            resetMovement(online);
            Component screen = this.plugin.alerts().color(
                    this.plugin.fuftiConfig().message("kick-screen").replace("%reason%", reason)
            );
            FuftiACCombatUtil.kick(online, screen);
        }
    }

    private void dispatch(String command, String check, String name) {
        String trimmed = command == null ? "" : command.trim();
        Component broadcast = this.plugin.alerts().color(
                this.plugin.fuftiConfig().message("punish-broadcast")
                        .replace("%player%", name)
                        .replace("%check%", check)
        );
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("fuftiac.alerts")) {
                FuftiACCombatUtil.sendMessage(staff, broadcast);
            }
        }
        Player player = Bukkit.getPlayerExact(name);
        if (player != null) {
            resetMovement(player);
        }
        if (trimmed.isEmpty()) {
            if (player != null) {
                FuftiACCombatUtil.kick(player, this.plugin.alerts().color(
                        this.plugin.fuftiConfig().message("kick-screen").replace("%reason%", check)
                ));
            }
            return;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length >= 4 && parts[0].equalsIgnoreCase("fuftiac") && parts[1].equalsIgnoreCase("ban")) {
            Duration duration = FuftiACTime.parse(parts[3]);
            String reason = parts.length > 4 ? String.join(" ", Arrays.copyOfRange(parts, 4, parts.length)) : check;
            applyBan(parts[2], duration, reason, true);
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed);
        Player still = Bukkit.getPlayerExact(name);
        if (still != null && still.isOnline()) {
            resetMovement(still);
        }
    }

    private static PotionEffectType getDarknessEffect() {
        PotionEffectType type = PotionEffectType.getByName("DARKNESS");
        return type != null ? type : PotionEffectType.BLINDNESS;
    }

    private static Particle getSmokeParticle() {
        try {
            return Particle.valueOf("SMOKE");
        } catch (Throwable e) {
            try {
                return Particle.valueOf("SMOKE_NORMAL");
            } catch (Throwable ignored) {
                return Particle.CLOUD;
            }
        }
    }
}
