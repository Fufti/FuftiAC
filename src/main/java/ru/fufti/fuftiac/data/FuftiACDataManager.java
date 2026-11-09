package ru.fufti.fuftiac.data;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FuftiACDataManager {

    private final Map<UUID, FuftiACPlayerData> dataMap = new ConcurrentHashMap<>();

    public FuftiACPlayerData get(Player player) {
        return get(player.getUniqueId());
    }

    public FuftiACPlayerData get(UUID uuid) {
        return this.dataMap.computeIfAbsent(uuid, FuftiACPlayerData::new);
    }

    public FuftiACPlayerData peek(UUID uuid) {
        return this.dataMap.get(uuid);
    }

    public void remove(Player player) {
        this.dataMap.remove(player.getUniqueId());
    }

    public void clear() {
        this.dataMap.clear();
    }
}
