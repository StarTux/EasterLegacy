package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.item.ItemDescription;
import com.winthier.custom.item.UncraftableItem;
import com.winthier.custom.util.Dirty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

@Getter @RequiredArgsConstructor
public class EasterEggItem implements CustomItem, UncraftableItem {
    private final EasterPlugin plugin;
    private final String customId = EasterPlugin.CUSTOM_ID_EASTER_EGG;
    private ItemDescription itemDescription;

    ItemDescription getItemDescription() {
        if (itemDescription == null) {
            itemDescription = new ItemDescription();
            itemDescription.load(plugin.getConfig().getConfigurationSection("egg"));
        }
        return itemDescription;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, amount, (short)3);
        Head head = plugin.getRandomEasterEggHead();
        item = Dirty.setSkullOwner(item, head.getName(), head.getId(), head.getTexture());
        getItemDescription().apply(item);
        return item;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event, ItemContext context) {
        if (!plugin.isEasterWorld(event.getBlock().getWorld())) {
            CustomPlugin.getInstance().getBlockManager().wrapBlock(event.getBlock(), EasterPlugin.CUSTOM_ID_EASTER_EGG);
        } else {
            event.setCancelled(true);
        }
    }
}
