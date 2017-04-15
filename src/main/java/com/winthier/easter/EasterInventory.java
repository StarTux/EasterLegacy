package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.inventory.CustomInventory;
import com.winthier.custom.item.CustomItem;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

@Getter
public class EasterInventory implements CustomInventory {
    private final EasterPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    EasterInventory(EasterPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.getServer().createInventory(player, 6 * 9, "Got any easter eggs?");
    }

    @Override
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(false);
    }

    @Override
    public void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(false);
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        int easterEggCount = 0;
        for (ItemStack item: inventory) {
            if (item == null || item.getAmount() < 1) continue;
            CustomItem customItem = CustomPlugin.getInstance().getItemManager().getCustomItem(item);
            if (customItem == null || !customItem.getCustomId().equals(EasterPlugin.CUSTOM_ID_EASTER_EGG)) {
                player.getWorld().dropItem(player.getEyeLocation(), item).setPickupDelay(0);
            } else {
                easterEggCount += item.getAmount();
            }
        }
        if (easterEggCount > 0) {
            plugin.rewardPlayerForEasterEggs(player, easterEggCount);
        }
    }
}
