package ru.fufti.fuftiac.check;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import ru.fufti.fuftiac.FuftiAC;
import ru.fufti.fuftiac.data.FuftiACPlayerData;

public final class FuftiACGuiMove extends FuftiACCheck implements Listener {

    public FuftiACGuiMove(FuftiAC plugin) {
        super(plugin, "GuiMove");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            FuftiACPlayerData data = this.plugin.data().get(player);
            data.setGuiOpen(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            FuftiACPlayerData data = this.plugin.data().get(player);
            data.setGuiOpen(false);
            data.setGuiMoveTicks(0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !player.isOnline()) {
            return;
        }

        double speed = data.speed();
        boolean hasMovement = speed > 0.08;
        boolean recentMove = System.currentTimeMillis() - data.lastMoveMs() < 120L;

        if (hasMovement && recentMove) {
            data.setGuiMoveTicks(data.guiMoveTicks() + 1);
            if (data.guiMoveTicks() >= 2) {
                org.bukkit.inventory.ItemStack clicked = event.getCurrentItem();
                if (clicked != null && !clicked.getType().isAir()) {
                    event.setCurrentItem(null);
                    player.getWorld().dropItemNaturally(player.getLocation(), clicked);
                }
                flag(player, data, "FuftiACGuiMove", "item click while moving in any direction speed=" + String.format(java.util.Locale.US, "%.2f", speed));
                data.setGuiMoveTicks(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        FuftiACPlayerData data = this.plugin.data().get(player);
        if (exempt(player, data) || !player.isOnline()) {
            return;
        }

        double speed = data.speed();
        boolean hasMovement = speed > 0.08;
        boolean recentMove = System.currentTimeMillis() - data.lastMoveMs() < 120L;

        if (hasMovement && recentMove) {
            data.setGuiMoveTicks(data.guiMoveTicks() + 1);
            if (data.guiMoveTicks() >= 2) {
                org.bukkit.inventory.ItemStack oldCursor = event.getOldCursor();
                if (oldCursor != null && !oldCursor.getType().isAir()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), oldCursor);
                }
                flag(player, data, "FuftiACGuiMove", "item drag while moving in any direction speed=" + String.format(java.util.Locale.US, "%.2f", speed));
                data.setGuiMoveTicks(0);
            }
        }
    }
}
