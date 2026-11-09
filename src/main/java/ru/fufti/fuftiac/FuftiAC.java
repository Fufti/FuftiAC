package ru.fufti.fuftiac;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.fufti.fuftiac.check.FuftiACCheckManager;
import ru.fufti.fuftiac.command.FuftiACCommand;
import ru.fufti.fuftiac.data.FuftiACDataManager;
import ru.fufti.fuftiac.punish.FuftiACPunish;
import ru.fufti.fuftiac.util.FuftiACScheduler;
import ru.fufti.fuftiac.util.FuftiACTask;

public final class FuftiAC extends JavaPlugin {

    private static FuftiAC instance;

    private FuftiACConfig fuftiConfig;
    private FuftiACDataManager dataManager;
    private FuftiACAlerts alerts;
    private FuftiACPunish punish;
    private FuftiACCheckManager checkManager;
    private FuftiACTask tickTask;
    private boolean packetEventsReady;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .checkForUpdates(false)
                .bStats(false)
                .reEncodeByDefault(false);
        if (Bukkit.getWorlds().isEmpty()) {
            PacketEvents.getAPI().load();
            this.packetEventsReady = true;
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.fuftiConfig = new FuftiACConfig(this);
        this.dataManager = new FuftiACDataManager();
        this.alerts = new FuftiACAlerts(this);
        this.punish = new FuftiACPunish(this);
        this.checkManager = new FuftiACCheckManager(this);

        if (!this.packetEventsReady) {
            getLogger().warning("PacketEvents maps load on the main thread. Restart the server instead of PlugMan.");
            PacketEvents.getAPI().load();
            this.packetEventsReady = true;
        }
        PacketEvents.getAPI().init();
        this.checkManager.register();

        FuftiACCommand command = new FuftiACCommand(this);
        var fuftiac = getCommand("fuftiac");
        if (fuftiac != null) {
            fuftiac.setExecutor(command);
            fuftiac.setTabCompleter(command);
        }
        var alertsCmd = getCommand("alerts");
        if (alertsCmd != null) {
            alertsCmd.setExecutor(command);
            alertsCmd.setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(new FuftiACJoinListener(this), this);
        this.tickTask = FuftiACScheduler.runGlobalTimer(this, this.checkManager::tick, 1L, 1L);

        getLogger().info("FuftiAC enabled (1.16.5-26.3+ Java 21, Folia=" + FuftiACScheduler.isFolia() + "), punishments=" + this.fuftiConfig.punishmentsEnabled());
    }

    @Override
    public void onDisable() {
        if (this.tickTask != null) {
            this.tickTask.cancel();
            this.tickTask = null;
        }
        if (this.checkManager != null) {
            this.checkManager.shutdown();
        }
        if (this.dataManager != null) {
            this.dataManager.clear();
        }
        try {
            PacketEvents.getAPI().terminate();
        } catch (Exception ignored) {
        }
        instance = null;
    }

    public static FuftiAC get() {
        return instance;
    }

    public FuftiACConfig fuftiConfig() {
        return this.fuftiConfig;
    }

    public FuftiACDataManager data() {
        return this.dataManager;
    }

    public FuftiACAlerts alerts() {
        return this.alerts;
    }

    public FuftiACPunish punish() {
        return this.punish;
    }

    public FuftiACCheckManager checks() {
        return this.checkManager;
    }
}
