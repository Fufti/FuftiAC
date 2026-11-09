package ru.fufti.fuftiac.check;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACCombatUtil;

public final class FuftiACCheckManager implements Listener {

    private final FuftiAC plugin;
    private final FuftiACElytraSwap elytraSwap;
    private final FuftiACGuiMove guiMove;
    private final FuftiACTargetStrafe targetStrafe;
    private final FuftiACScaffold scaffold;
    private final FuftiACSpider spider;
    private final FuftiACNpc npc;
    private final FuftiACHitbox hitbox;
    private final FuftiACReach reach;
    private final FuftiACNoWeb noWeb;
    private final FuftiACSpeed speed;
    private final FuftiACWallHit wallHit;
    private final FuftiACNoSlow noSlow;
    private final FuftiACFly fly;
    private final FuftiACJesus jesus;
    private final FuftiACNoFall noFall;
    private final FuftiACStep step;
    private final FuftiACAirJump airJump;
    private final FuftiACVelocity velocity;
    private final FuftiACVelocityLegit velocityLegit;
    private final FuftiACBoatFly boatFly;
    private final FuftiACNoClip noClip;
    private final FuftiACElytraRecast elytraRecast;
    private final FuftiACAirStuck airStuck;
    private final FuftiACNuker nuker;
    private int tick;

    public FuftiACCheckManager(FuftiAC plugin) {
        this.plugin = plugin;
        this.elytraSwap = new FuftiACElytraSwap(plugin);
        this.guiMove = new FuftiACGuiMove(plugin);
        this.targetStrafe = new FuftiACTargetStrafe(plugin);
        this.scaffold = new FuftiACScaffold(plugin);
        this.spider = new FuftiACSpider(plugin);
        this.npc = new FuftiACNpc(plugin);
        this.hitbox = new FuftiACHitbox(plugin);
        this.reach = new FuftiACReach(plugin);
        this.noWeb = new FuftiACNoWeb(plugin);
        this.speed = new FuftiACSpeed(plugin);
        this.wallHit = new FuftiACWallHit(plugin);
        this.noSlow = new FuftiACNoSlow(plugin);
        this.fly = new FuftiACFly(plugin);
        this.jesus = new FuftiACJesus(plugin);
        this.noFall = new FuftiACNoFall(plugin);
        this.step = new FuftiACStep(plugin);
        this.airJump = new FuftiACAirJump(plugin);
        this.velocity = new FuftiACVelocity(plugin);
        this.velocityLegit = new FuftiACVelocityLegit(plugin);
        this.boatFly = new FuftiACBoatFly(plugin);
        this.noClip = new FuftiACNoClip(plugin);
        this.elytraRecast = new FuftiACElytraRecast(plugin);
        this.airStuck = new FuftiACAirStuck(plugin);
        this.nuker = new FuftiACNuker(plugin);
    }

    public void register() {
        var pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(this, this.plugin);
        pluginManager.registerEvents(this.elytraSwap, this.plugin);
        pluginManager.registerEvents(this.guiMove, this.plugin);
        pluginManager.registerEvents(this.scaffold, this.plugin);
        pluginManager.registerEvents(this.spider, this.plugin);
        pluginManager.registerEvents(this.velocity, this.plugin);
        pluginManager.registerEvents(this.boatFly, this.plugin);
        pluginManager.registerEvents(this.nuker, this.plugin);
        pluginManager.registerEvents(new FuftiACMoveListener(this.plugin), this.plugin);
        PacketEvents.getAPI().getEventManager().registerListener(new FuftiACPacketListener(this.plugin));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        handleCombat(player, target, event);
    }

    public void handleCombat(Player player, int entityId) {
        LivingEntity target = FuftiACCombatUtil.find(player, entityId);
        if (target != null) {
            handleCombat(player, target, null);
        }
    }

    public void handleCombat(Player player, LivingEntity target, EntityDamageByEntityEvent event) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        data.markAttack(target.getUniqueId());
        this.npc.onCombat(player);

        if (player.getBoundingBox().overlaps(target.getBoundingBox())
                || FuftiACCombatUtil.reach(player, target) < 0.6) {
            return;
        }

        if (System.currentTimeMillis() - data.lastCombatCheckMs() < 25L) {
            return;
        }
        data.markCombatCheck();

        boolean cancel = false;
        if (this.hitbox.handleAttack(player, target, event)) {
            cancel = true;
        }
        if (this.reach.handleAttack(player, target, event)) {
            cancel = true;
        }
        if (this.wallHit.handleAttack(player, target, event)) {
            cancel = true;
        }
        if (cancel && event != null) {
            event.setCancelled(true);
        }
    }

    public void tick() {
        this.tick++;
        boolean npcTick = this.tick % 2 == 0;
        boolean decayTick = this.tick % 20 == 0;
        long decay = this.plugin.fuftiConfig().vlDecaySeconds() * 1000L;
        for (Player player : Bukkit.getOnlinePlayers()) {
            FuftiACPlayerData data = this.plugin.data().get(player);
            if (decayTick) {
                data.decay(decay);
            }
            if (data.punishing()) {
                continue;
            }
            if (npcTick) {
                this.npc.tick(player, data);
            }
        }
    }

    public void shutdown() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.npc.remove(player);
        }
    }

    public FuftiACNpc npc() {
        return this.npc;
    }

    public FuftiACTargetStrafe targetStrafe() {
        return this.targetStrafe;
    }

    public FuftiACSpider spider() {
        return this.spider;
    }

    public FuftiACSpeed speed() {
        return this.speed;
    }

    public FuftiACNoWeb noWeb() {
        return this.noWeb;
    }

    public FuftiACNoSlow noSlow() {
        return this.noSlow;
    }

    public FuftiACElytraSwap elytraSwap() {
        return this.elytraSwap;
    }

    public FuftiACGuiMove guiMove() {
        return this.guiMove;
    }

    public FuftiACHitbox hitbox() {
        return this.hitbox;
    }

    public FuftiACWallHit wallHit() {
        return this.wallHit;
    }

    public FuftiACScaffold scaffold() {
        return this.scaffold;
    }

    public FuftiACReach reach() {
        return this.reach;
    }

    public FuftiACFly fly() {
        return this.fly;
    }

    public FuftiACJesus jesus() {
        return this.jesus;
    }

    public FuftiACNoFall noFall() {
        return this.noFall;
    }

    public FuftiACStep step() {
        return this.step;
    }

    public FuftiACAirJump airJump() {
        return this.airJump;
    }

    public FuftiACVelocity velocity() {
        return this.velocity;
    }

    public FuftiACVelocityLegit velocityLegit() {
        return this.velocityLegit;
    }

    public FuftiACBoatFly boatFly() {
        return this.boatFly;
    }

    public FuftiACNoClip noClip() {
        return this.noClip;
    }

    public FuftiACElytraRecast elytraRecast() {
        return this.elytraRecast;
    }

    public FuftiACAirStuck airStuck() {
        return this.airStuck;
    }

    public FuftiACNuker nuker() {
        return this.nuker;
    }
}
