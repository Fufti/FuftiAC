package ru.fufti.fuftiac.check;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class FuftiACNpc extends FuftiACCheck {

    private static final AtomicInteger ENTITY_IDS = new AtomicInteger(1_200_000_000);

    public FuftiACNpc(FuftiAC plugin) {
        super(plugin, "NPC");
    }

    public void handleHit(Player player) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !enabled()) {
            return;
        }
        long window = this.plugin.fuftiConfig().millis(name(), "window-ms", 30_000L);
        data.addNpcHit();
        int need = this.plugin.fuftiConfig().integer(name(), "hits", 2);
        if (data.npcHitsIn(window) >= need) {
            data.resetNpcHits();
            data.setNpcWindows(data.npcWindows() + 1);
            flag(player, data, "FuftiACNpc", "hits=" + need + " in " + (window / 1000) + "s");
        }
    }

    public void onCombat(Player player) {
        if (!enabled() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (!data.npcSpawned()) {
            spawn(player);
        }
    }

    public void spawn(Player player) {
        if (!enabled() || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (data.npcSpawned()) {
            return;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            return;
        }

        int entityId = ENTITY_IDS.getAndIncrement();
        UUID uuid = UUID.randomUUID();
        String name = "N" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0xFFFFF));
        UserProfile profile = new UserProfile(uuid, name);
        data.setNpcEntityId(entityId);
        data.setNpcUuid(uuid);

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile,
                true,
                0,
                GameMode.SURVIVAL,
                null,
                null
        );
        user.sendPacket(new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(
                        WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED
                ),
                List.of(info)
        ));

        Location pos = behind(player);
        user.sendPacket(new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.PLAYER,
                new Vector3d(pos.getX(), pos.getY(), pos.getZ()),
                pos.getPitch(),
                pos.getYaw(),
                pos.getYaw(),
                0,
                Optional.empty()
        ));

        if (this.plugin.fuftiConfig().flag(name(), "invisible", true)) {
            List<EntityData> meta = new ArrayList<>();
            meta.add(new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20));
            user.sendPacket(new WrapperPlayServerEntityMetadata(entityId, meta));
        }
        data.setNpcSpawned(true);
    }

    public void tick(Player player, FuftiACPlayerData data) {
        if (!enabled() || !player.isOnline()) {
            return;
        }
        long combatMs = this.plugin.fuftiConfig().millis(name(), "combat-ms", 8000L);
        boolean combat = System.currentTimeMillis() - data.lastAttackMs() <= combatMs;
        if (!combat) {
            if (data.npcSpawned()) {
                remove(player);
            }
            return;
        }
        if (!data.npcSpawned() || data.npcEntityId() < 0) {
            spawn(player);
            return;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            return;
        }
        Location loc = behind(player);
        user.sendPacket(new WrapperPlayServerEntityTeleport(
                data.npcEntityId(),
                new com.github.retrooper.packetevents.protocol.world.Location(
                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()
                ),
                false
        ));
    }

    public void remove(Player player) {
        FuftiACPlayerData data = this.plugin.data().peek(player.getUniqueId());
        if (data == null || !data.npcSpawned()) {
            return;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user != null) {
            try {
                user.sendPacket(new WrapperPlayServerDestroyEntities(data.npcEntityId()));
                if (data.npcUuid() != null) {
                    user.sendPacket(new WrapperPlayServerPlayerInfoRemove(List.of(data.npcUuid())));
                }
            } catch (Exception ignored) {
            }
        }
        data.setNpcSpawned(false);
        data.setNpcEntityId(-1);
        data.setNpcUuid(null);
    }

    private Location behind(Player player) {
        double dist = this.plugin.fuftiConfig().decimal(name(), "radius", 2.4);
        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection().setY(0);
        if (dir.lengthSquared() < 0.001) {
            dir = new Vector(0, 0, -1);
        } else {
            dir.normalize();
        }
        Location loc = player.getLocation().clone();
        loc.subtract(dir.multiply(dist));
        loc.add(0, 0.25, 0);
        loc.setYaw(player.getLocation().getYaw());
        loc.setPitch(0.0f);
        return loc;
    }
}
