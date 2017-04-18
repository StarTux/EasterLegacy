package com.winthier.easter;

import com.winthier.custom.block.BlockContext;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.block.CustomBlock;
import com.winthier.custom.block.TickableBlock;
import com.winthier.custom.block.UnbreakableBlock;
import com.winthier.custom.util.Msg;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

@Getter @RequiredArgsConstructor
abstract class EasterBlock implements CustomBlock, TickableBlock, UnbreakableBlock {
    protected final EasterPlugin plugin;

    enum Type {
        EGG, BASKET;
    }

    public abstract Type getType();

    @Override
    public void setBlock(Block block) {
        Entity sender = null;
        for (Player player: block.getWorld().getPlayers()) {
            if (player.getWorld().equals(block.getWorld())) {
                sender = player;
                break;
            }
        }
        if (sender == null || !sender.isValid()) {
            for (Entity entity: block.getWorld().getEntities()) {
                if (entity.getWorld().equals(block.getWorld())) {
                    sender = entity;
                    break;
                }
            }
        }
        if (sender == null || !sender.isValid()) {
            sender = block.getWorld().spawn(block.getWorld().getSpawnLocation(), ExperienceOrb.class);
        }
        if (sender == null || !sender.isValid()) {
            block.setType(Material.SKULL);
        } else {
            int rotation = plugin.getRandom().nextInt(16);
            Head head;
            switch (getType()) {
            case EGG: head = plugin.getRandomEasterEggHead();
                break;
            case BASKET: head = plugin.getRandomEasterBasketHead();
                break;
            default: return;
            }
            Msg.consoleCommand("minecraft:execute %s ~ ~ ~ minecraft:setblock %d %d %d minecraft:skull 1 replace {SkullType:3,Rot:%d,Owner:{Id:%s,Name:%s,Properties:{textures:[{Value:%s}]}}}",
                               sender.getUniqueId(),
                               block.getX(), block.getY(), block.getZ(), rotation,
                               head.getId(), head.getName(), head.getTexture());
        }
    }

    @Override
    public BlockWatcher createBlockWatcher(Block block) {
        return new Watcher(block, this);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event, BlockContext context) {
    }

    @Override @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event, BlockContext context) {
        event.setInstaBreak(true);
    }

    @Override
    public void onTick(BlockWatcher watcher) {
        if (!plugin.isEasterWorld(watcher.getBlock().getWorld())) return;
        ((Watcher)watcher).onTick();
    }

    @Getter @RequiredArgsConstructor
    final class Watcher implements BlockWatcher {
        private final Block block;
        private final EasterBlock customBlock;
        private int ticks;

        void onTick() {
            if (ticks == 0) {
                plugin.getEasterBlocks().add(block);
                ticks = plugin.getRandom().nextInt(20 * 10);
                block.getWorld().playSound(block.getLocation().add(0.5, 0.5, 0.5), Sound.ENTITY_RABBIT_HURT, SoundCategory.BLOCKS, 2.0f, 0.1f);
            } else {
                ticks -= 1;
            }
        }
    }
}
