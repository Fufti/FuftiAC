package ru.fufti.fuftiac.check;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACScheduler;

import java.util.UUID;

public final class FuftiACPacketListener extends PacketListenerAbstract {

    private final FuftiAC plugin;

    public FuftiACPacketListener(FuftiAC plugin) {
        super(PacketListenerPriority.HIGH);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_FLYING) {
            UUID uuid = event.getUser() == null ? null : event.getUser().getUUID();
            if (uuid != null) {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
                FuftiACPlayerData data = this.plugin.data().get(uuid);
                data.setClientOnGround(flying.isOnGround());
                if (flying.hasPositionChanged()) {
                    double dy = flying.getLocation().getY() - data.lastY();
                    if (dy > 0.001 && dy < 0.12 && !flying.isOnGround()) {
                        data.markPacketMicroHop();
                    }
                }
                this.plugin.checks().speed().onFlyingPacket(uuid);
            }
            return;
        }
        if (type != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }
        try {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }
            UUID uuid = event.getUser() == null ? null : event.getUser().getUUID();
            if (uuid == null) {
                return;
            }
            int entityId = packet.getEntityId();
            FuftiACPlayerData data = this.plugin.data().get(uuid);
            boolean npcHit = data.npcEntityId() == entityId && entityId > 0;
            if (npcHit) {
                event.setCancelled(true);
            }
            FuftiACScheduler.runTask(this.plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    return;
                }
                if (npcHit) {
                    this.plugin.checks().npc().handleHit(player);
                } else {
                    this.plugin.checks().handleCombat(player, entityId);
                }
            });
        } catch (Exception ignored) {
        }
    }
}
