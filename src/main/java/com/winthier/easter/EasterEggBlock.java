package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;

@Getter
public final class EasterEggBlock extends EasterBlock {
    private final String customId = EasterPlugin.CUSTOM_ID_EASTER_EGG;
    private final Type type = Type.EGG;

    EasterEggBlock(EasterPlugin plugin) {
        super(plugin);
    }

    @Override @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event, BlockContext context) {
        plugin.getEasterBlocks().remove(context.getBlock());
        CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(context.getBlockWatcher());
        plugin.wrapDroppedItem(context.getBlock());
        if (plugin.isEasterWorld(context.getBlock().getWorld())) {
            plugin.spawnNewBlock(context.getBlock());
            plugin.playJingle(context.getBlock().getLocation());
        }
    }
}
