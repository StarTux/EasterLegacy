package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

@Getter
public final class EasterBasketBlock extends EasterBlock {
    private final String customId = EasterPlugin.CUSTOM_ID_EASTER_BASKET;
    private final Type type = Type.BASKET;

    EasterBasketBlock(EasterPlugin plugin) {
        super(plugin);
    }

    @Override @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event, BlockContext context) {
        CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(context.getBlockWatcher());
        event.setCancelled(true);
        context.getBlock().setType(Material.AIR);
        Location loc = context.getBlock().getLocation().add(0.5, 0.0, 0.5);
        plugin.getEasterBlocks().remove(context.getBlock());
        plugin.basketFound(event.getPlayer(), loc);
    }
}
