package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import lombok.Getter;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

@Getter
public final class EasterBasketBlock extends EasterBlock {
    private final String customId = EasterPlugin.CUSTOM_ID_EASTER_BASKET;
    private final Type type = Type.BASKET;

    EasterBasketBlock(EasterPlugin plugin) {
        super(plugin);
    }

    @Override @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event, BlockContext context) {
        plugin.getEasterBlocks().remove(context.getBlock());
        CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(context.getBlockWatcher());
        event.setCancelled(true);
        context.getBlock().setType(Material.AIR);
        plugin.spawnNewBlock(context.getBlock());
        Location loc = context.getBlock().getLocation().add(0.5, 0.0, 0.5);
        plugin.playJingle(loc);
        for (int i = 0; i < 10; i += 1) {
            switch (plugin.getRandom().nextInt(3)) {
            case 0:
                Item item = CustomPlugin.getInstance().getItemManager().dropItemStack(loc, EasterPlugin.CUSTOM_ID_EASTER_EGG, 1);
                item.setVelocity(new Vector(plugin.getRandom().nextDouble() * 1.0 - 0.5,
                                            plugin.getRandom().nextDouble() * 1.0,
                                            plugin.getRandom().nextDouble() * 1.0 - 0.5));
                break;
            case 1:
                Rabbit rabbit = loc.getWorld().spawn(loc, Rabbit.class, new Consumer<Rabbit>() {
                    @Override public void accept(Rabbit rabbit) {
                        Rabbit.Type type = Rabbit.Type.values()[plugin.getRandom().nextInt(Rabbit.Type.values().length)];
                        rabbit.setRabbitType(type);
                        rabbit.setRemoveWhenFarAway(true);
                        rabbit.setVelocity(new Vector(plugin.getRandom().nextDouble() * 1.0 - 0.5,
                                                      plugin.getRandom().nextDouble() * 0.5,
                                                      plugin.getRandom().nextDouble() * 1.0 - 0.5));
                    }
                });
                break;
            case 2:
                Sheep sheep = loc.getWorld().spawn(loc, Sheep.class, new Consumer<Sheep>() {
                    @Override public void accept(Sheep sheep) {
                        DyeColor type = DyeColor.values()[plugin.getRandom().nextInt(DyeColor.values().length)];
                        sheep.setColor(type);
                        sheep.setBaby();
                        sheep.setRemoveWhenFarAway(true);
                        sheep.setVelocity(new Vector(plugin.getRandom().nextDouble() * 1.0 - 0.5,
                                                     plugin.getRandom().nextDouble() * 0.5,
                                                     plugin.getRandom().nextDouble() * 1.0 - 0.5));
                    }
                });
            default:
                break;
            }
        }
    }
}
