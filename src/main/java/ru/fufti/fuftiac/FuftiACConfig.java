package ru.fufti.fuftiac;

import org.bukkit.configuration.file.FileConfiguration;

public final class FuftiACConfig {

    public static final String PREFIX = "<dark_gray>[</dark_gray><gradient:#38BDF8:#818CF8><bold>FuftiAC</bold></gradient><dark_gray>]</dark_gray>";

    private final FuftiAC plugin;
    private FileConfiguration cfg;

    public FuftiACConfig(FuftiAC plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.plugin.reloadConfig();
        this.cfg = this.plugin.getConfig();
    }

    public FileConfiguration raw() {
        return this.cfg;
    }

    public String prefix() {
        return PREFIX;
    }

    public String alertFormat() {
        return this.cfg.getString("alerts.format", "%prefix% <white>%player%</white> <gray>флагнул</gray> <gradient:#38BDF8:#818CF8>%check%</gradient> <dark_gray>[<white>%vl%</white><gray>/</gray><gray>%max%</gray>]</dark_gray> <dark_gray>›</dark_gray> <gray>%detail%</gray>");
    }

    public String alertHover() {
        return this.cfg.getString("alerts.hover", "<gradient:#38BDF8:#818CF8><bold>%check%</bold></gradient>\n<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n<gray>Игрок:</gray> <white>%player%</white>\n<gray>Нарушение:</gray> <aqua>%vl%</aqua><gray>/</gray><gray>%max%</gray>\n<gray>Пинг:</gray> <white>%ping%</white><gray>ms</gray>\n<gray>Координаты:</gray> <white>%x% %y% %z%</white> <dark_gray>(%world%)</dark_gray>\n<gray>Инфо:</gray> <white>%detail%</white>\n<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>\n<yellow>Клик — телепортироваться</yellow>");
    }

    public String message(String key) {
        return this.cfg.getString("messages." + key, PREFIX + " <red>" + key + "</red>");
    }

    public int vlDecaySeconds() {
        return this.cfg.getInt("vl-decay-seconds", 120);
    }

    public boolean animationEnabled() {
        return this.cfg.getBoolean("animation.enabled", false);
    }

    public int animationTicks() {
        return this.cfg.getInt("animation.ticks", 45);
    }

    public double riseSpeed() {
        return this.cfg.getDouble("animation.rise-speed", 0.38);
    }

    public boolean animationSound() {
        return this.cfg.getBoolean("animation.sound", false);
    }

    public boolean animationParticles() {
        return this.cfg.getBoolean("animation.particles", false);
    }

    public boolean punishmentsEnabled() {
        return this.cfg.getBoolean("punishments.enabled", false);
    }

    public void setPunishmentsEnabled(boolean enabled) {
        this.cfg.set("punishments.enabled", enabled);
        this.plugin.saveConfig();
        this.cfg = this.plugin.getConfig();
    }

    public boolean checkEnabled(String check) {
        String key = check.startsWith("FuftiAC") ? check : "FuftiAC" + check;
        if (this.cfg.isSet("checks." + key + ".enabled")) {
            return this.cfg.getBoolean("checks." + key + ".enabled", true);
        }
        return this.cfg.getBoolean("checks." + check + ".enabled", true);
    }

    public int maxVl(String check) {
        String key = check.startsWith("FuftiAC") ? check : "FuftiAC" + check;
        if (this.cfg.isSet("checks." + key + ".max-vl")) {
            return Math.max(1, this.cfg.getInt("checks." + key + ".max-vl", 5));
        }
        return Math.max(1, this.cfg.getInt("checks." + check + ".max-vl", 5));
    }

    public String punishCommand(String check) {
        String key = check.startsWith("FuftiAC") ? check : "FuftiAC" + check;
        if (this.cfg.isSet("checks." + key + ".punish")) {
            return this.cfg.getString("checks." + key + ".punish", "");
        }
        return this.cfg.getString("checks." + check + ".punish", "");
    }

    public boolean flag(String check, String key, boolean def) {
        String cKey = check.startsWith("FuftiAC") ? check : "FuftiAC" + check;
        if (this.cfg.isSet("checks." + cKey + "." + key)) {
            return this.cfg.getBoolean("checks." + cKey + "." + key, def);
        }
        return this.cfg.getBoolean("checks." + check + "." + key, def);
    }

    public int integer(String check, String key, int def) {
        String cKey = check.startsWith("FuftiAC") ? check : "FuftiAC" + check;
        if (this.cfg.isSet("checks." + cKey + "." + key)) {
            return this.cfg.getInt("checks." + cKey + "." + key, def);
        }
        return this.cfg.getInt("checks." + check + "." + key, def);
    }

    public double decimal(String check, String key, double def) {
        String cKey = check.startsWith("FuftiAC") ? check : "FuftiAC" + check;
        if (this.cfg.isSet("checks." + cKey + "." + key)) {
            return this.cfg.getDouble("checks." + cKey + "." + key, def);
        }
        return this.cfg.getDouble("checks." + check + "." + key, def);
    }

    public long millis(String check, String key, long def) {
        String cKey = check.startsWith("FuftiAC") ? check : "FuftiAC" + check;
        if (this.cfg.isSet("checks." + cKey + "." + key)) {
            return this.cfg.getLong("checks." + cKey + "." + key, def);
        }
        return this.cfg.getLong("checks." + check + "." + key, def);
    }
}
