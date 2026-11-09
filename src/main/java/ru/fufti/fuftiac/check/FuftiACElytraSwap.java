package ru.fufti.fuftiac.check;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;
import ru.fufti.fuftiac.util.FuftiACScheduler;

public final class FuftiACElytraSwap extends FuftiACCheck implements Listener {

    public FuftiACElytraSwap(FuftiAC plugin) {
        super(plugin, "ElytraSwap");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            return;
        }
        if (item.getType() == Material.ELYTRA || isChestplate(item.getType())) {
            this.plugin.data().get(event.getPlayer()).markVanillaElytraEquip();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack before = player.getInventory().getChestplate();
        FuftiACScheduler.runEntityTask(this.plugin, player, () -> {
            if (!player.isOnline()) {
                return;
            }
            ItemStack after = player.getInventory().getChestplate();
            if (changedElytra(before, after)) {
                checkSwap(player);
            }
        });
    }

    public void checkMove(Player player, FuftiACPlayerData data) {
        ItemStack chest = player.getInventory().getChestplate();
        if (changedElytra(data.lastChest(), chest)) {
            checkSwap(player);
        }
        data.setLastChest(cloneOf(chest));
    }

    private void checkSwap(Player player) {
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - data.lastElytraFlagMs() < 400L) {
            return;
        }

        if (now - data.lastVanillaElytraEquip() < 1000L) {
            data.markElytraSwap();
            return;
        }

        long interval = now - data.lastElytraSwapTime();
        data.markElytraSwap();

        double speed = data.speed();
        double minSpeed = this.plugin.fuftiConfig().decimal(name(), "min-speed", 0.16);
        boolean activelyMoving = speed >= minSpeed && (now - data.lastMoveMs() < 120L);
        boolean sprinting = player.isSprinting() && speed > 0.12;
        boolean gliding = player.isGliding();

        boolean illegal = false;
        String reason = "";

        if (interval < 200L && (activelyMoving || sprinting || gliding)) {
            illegal = true;
            reason = "rapid swap in " + interval + "ms speed=" + String.format(java.util.Locale.US, "%.2f", speed);
        } else if (sprinting || (activelyMoving && speed > 0.18)) {
            illegal = true;
            reason = "swap while moving speed=" + String.format(java.util.Locale.US, "%.2f", speed);
        } else if (gliding && activelyMoving && speed > 0.30) {
            illegal = true;
            reason = "swap while gliding speed=" + String.format(java.util.Locale.US, "%.2f", speed);
        }

        if (illegal) {
            data.markElytraFlag();
            dropChestplate(player);
            flag(player, data, "FuftiACElytraSwap", reason);
        }
    }

    private void dropChestplate(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && !chest.getType().isAir()) {
            player.getInventory().setChestplate(null);
            player.getWorld().dropItemNaturally(player.getLocation(), chest);
        }
    }

    private static boolean changedElytra(ItemStack last, ItemStack now) {
        return isElytra(last) != isElytra(now);
    }

    private static ItemStack cloneOf(ItemStack item) {
        return item == null || item.getType().isAir() ? null : item.clone();
    }

    private static boolean isElytra(ItemStack item) {
        return item != null && !item.getType().isAir() && item.getType() == Material.ELYTRA;
    }

    private static boolean isChestplate(Material material) {
        return material.name().endsWith("_CHESTPLATE");
    }
}
