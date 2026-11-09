package ru.fufti.fuftiac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;
import ru.fufti.fuftiac.util.FuftiACText;

import java.util.Locale;

public final class FuftiACAlerts {

    private final FuftiAC plugin;

    public FuftiACAlerts(FuftiAC plugin) {
        this.plugin = plugin;
    }

    public void toggle(Player player) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        data.setAlerts(!data.alerts());
        String key = data.alerts() ? "alerts-on" : "alerts-off";
        FuftiACCombatUtil.sendMessage(player, color(this.plugin.fuftiConfig().message(key)));
    }

    public void flag(Player target, String check, int vl, int max, String detail) {
        String displayCheck = check;
        if (displayCheck != null && !displayCheck.startsWith("FuftiAC")) {
            displayCheck = "FuftiAC" + displayCheck;
        }
        Location loc = target.getLocation();
        String format = apply(this.plugin.fuftiConfig().alertFormat(), target, displayCheck, vl, max, detail, loc);
        String hover = apply(this.plugin.fuftiConfig().alertHover(), target, displayCheck, vl, max, detail, loc);

        Component message = FuftiACText.parse(format)
                .hoverEvent(HoverEvent.showText(FuftiACText.parse(hover)))
                .clickEvent(ClickEvent.suggestCommand("/tp " + target.getName()));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            FuftiACPlayerData data = this.plugin.data().get(staff);
            if (!data.alerts()) {
                continue;
            }
            FuftiACCombatUtil.sendMessage(staff, message);
        }
        this.plugin.getLogger().info("[FuftiAC] " + target.getName() + " " + displayCheck + " " + vl + "/" + max
                + (detail == null || detail.isEmpty() ? "" : " (" + detail + ")"));
    }

    public Component color(String text) {
        if (text == null) {
            return Component.empty();
        }
        return FuftiACText.parse(text.replace("%prefix%", this.plugin.fuftiConfig().prefix()));
    }

    private String apply(String template, Player target, String check, int vl, int max, String detail, Location loc) {
        return template
                .replace("%prefix%", this.plugin.fuftiConfig().prefix())
                .replace("%player%", target.getName())
                .replace("%check%", check)
                .replace("%vl%", String.valueOf(vl))
                .replace("%max%", String.valueOf(max))
                .replace("%bar%", FuftiACText.bar(vl, max))
                .replace("%detail%", detail == null || detail.isEmpty() ? "—" : detail)
                .replace("%ping%", String.valueOf(FuftiACCombatUtil.getPing(target)))
                .replace("%world%", loc.getWorld() == null ? "-" : loc.getWorld().getName())
                .replace("%x%", String.format(Locale.US, "%.1f", loc.getX()))
                .replace("%y%", String.format(Locale.US, "%.1f", loc.getY()))
                .replace("%z%", String.format(Locale.US, "%.1f", loc.getZ()));
    }
}
