package ru.fufti.fuftiac.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;
import ru.fufti.fuftiac.util.FuftiACText;
import ru.fufti.fuftiac.util.FuftiACTime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class FuftiACCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("ban", "unban", "info", "alerts", "punish", "reload");
    private static final List<String> DURATIONS = List.of("1s", "1m", "1h", "1d", "7d", "30d");
    private static final String[] CHECKS = {
            "ElytraSwap", "GuiMove", "TargetStrafe", "Scaffold", "Spider", "NPC",
            "Hitbox", "Reach", "NoWeb", "Speed", "WallHit", "NoSlow", "Fly", "Jesus",
            "NoFall", "Step", "AirJump", "AirStuck", "Velocity", "Velocity Legit",
            "BoatFly", "NoClip", "ElytraRecast", "Nuker"
    };

    private final FuftiAC plugin;

    public FuftiACCommand(FuftiAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("alerts")
                || (args.length > 0 && args[0].equalsIgnoreCase("alerts"))) {
            return toggleAlerts(sender);
        }

        if (args.length == 0) {
            send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("usage")));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            if (!has(sender, "fuftiac.admin")) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("no-permission")));
                return true;
            }
            this.plugin.fuftiConfig().reload();
            send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("reload")));
            return true;
        }

        if (sub.equals("ban")) {
            if (!has(sender, "fuftiac.ban")) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("no-permission")));
                return true;
            }
            if (args.length < 3) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("usage-ban")));
                return true;
            }
            Duration duration = FuftiACTime.parse(args[2]);
            if (duration == null) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("invalid-duration")));
                return true;
            }
            String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "FuftiAC";
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                this.plugin.punish().ban(sender, target, duration, reason);
            } else {
                this.plugin.punish().banOffline(sender, args[1], duration, reason);
            }
            return true;
        }

        if (sub.equals("unban")) {
            if (!has(sender, "fuftiac.unban") && !has(sender, "fuftiac.ban")) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("no-permission")));
                return true;
            }
            if (args.length < 2) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("usage-unban")));
                return true;
            }
            this.plugin.punish().unban(sender, args[1]);
            return true;
        }

        if (sub.equals("punish")) {
            if (!has(sender, "fuftiac.admin")) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("no-permission")));
                return true;
            }
            if (args.length >= 2) {
                String mode = args[1].toLowerCase(Locale.ROOT);
                if (mode.equals("on") || mode.equals("true") || mode.equals("enable")) {
                    this.plugin.fuftiConfig().setPunishmentsEnabled(true);
                } else if (mode.equals("off") || mode.equals("false") || mode.equals("disable")) {
                    this.plugin.fuftiConfig().setPunishmentsEnabled(false);
                }
            }
            boolean enabled = this.plugin.fuftiConfig().punishmentsEnabled();
            send(sender, this.plugin.alerts().color(
                    this.plugin.fuftiConfig().message(enabled ? "punish-on" : "punish-off")
            ));
            return true;
        }

        if (sub.equals("info")) {
            if (!has(sender, "fuftiac.info")) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("no-permission")));
                return true;
            }
            if (args.length < 2) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("usage-info")));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("player-not-found").replace("%player%", args[1])));
                return true;
            }
            FuftiACPlayerData data = this.plugin.data().get(target);
            send(sender, this.plugin.alerts().color(
                    this.plugin.fuftiConfig().message("info-header").replace("%player%", target.getName())
            ));
            for (String check : CHECKS) {
                int vl = data.vl(check);
                int max = this.plugin.fuftiConfig().maxVl(check);
                send(sender, this.plugin.alerts().color(
                        this.plugin.fuftiConfig().message("info-line")
                                .replace("%check%", check)
                                .replace("%vl%", String.valueOf(vl))
                                .replace("%max%", String.valueOf(max))
                                .replace("%bar%", FuftiACText.bar(vl, max))
                ));
            }
            return true;
        }

        send(sender, this.plugin.alerts().color(this.plugin.fuftiConfig().message("usage")));
        return true;
    }

    private boolean toggleAlerts(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }
        this.plugin.alerts().toggle(player);
        return true;
    }

    private static boolean has(CommandSender sender, String perm) {
        return sender.isOp() || sender.hasPermission(perm) || sender.hasPermission("fuftiac.admin");
    }

    private static void send(CommandSender sender, net.kyori.adventure.text.Component component) {
        if (sender instanceof Player player) {
            FuftiACCombatUtil.sendMessage(player, component);
        } else {
            sender.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("alerts")) {
            return List.of();
        }
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("ban") || sub.equals("info")) {
                return filter(onlinePlayers(), args[1]);
            }
            if (sub.equals("punish")) {
                return filter(List.of("on", "off"), args[1]);
            }
        }
        if (args.length == 3 && sub.equals("ban")) {
            return filter(DURATIONS, args[2]);
        }
        return List.of();
    }

    private static List<String> onlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private static List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String item : list) {
            if (item.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(item);
            }
        }
        return out;
    }
}
