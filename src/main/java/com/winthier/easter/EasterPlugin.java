package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.custom.util.Msg;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public final class EasterPlugin extends JavaPlugin implements Listener {
    static final String CUSTOM_ID_EASTER_EGG = "easter:easter_egg";
    static final String CUSTOM_ID_EASTER_BASKET = "easter:easter_basket";
    private List<Head> easterEggHeads;
    private List<Head> easterBasketHeads;
    private final Random random = new Random(System.currentTimeMillis());
    private String world;
    private final Set<Block> easterBlocks = new HashSet<>();
    private final Set<Block> wrapDroppedItems = new HashSet<>();
    private EasterEggItem easterEggItem;
    private YamlConfiguration scores;
    private List<RewardItem> rewardItems;
    private Map<Integer, RewardItem> rewards;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        reloadConfig();
        easterBlocks.clear();
        wrapDroppedItems.clear();
        scores = null;
        rewardItems = null;
        rewards = null;
        world = getConfig().getString("World");
        event.addBlock(new EasterEggBlock(this));
        event.addBlock(new EasterBasketBlock(this));
        easterEggItem = new EasterEggItem(this);
        event.addItem(easterEggItem);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = args.length == 0 ? null : args[0].toLowerCase();
        if (cmd == null) {
            return false;
        } else if (cmd.equals("inventory") && args.length == 2) {
            Player target = getServer().getPlayerExact(args[1]);
            CustomPlugin.getInstance().getInventoryManager().openInventory(target, new EasterInventory(this, target));
            sender.sendMessage("Opened inventory for " + target.getName());
        } else if (cmd.equals("info") && args.length == 2) {
            Player target = getServer().getPlayerExact(args[1]);
            target.sendMessage("So far, you have returned " + ChatColor.GREEN + getScore(target) + ChatColor.RESET + " easter eggs.");
        } else if (cmd.equals("config") && args.length == 1) {
            saveDefaultConfig();
            sender.sendMessage("Saved default config");
        } else if (cmd.equals("reward") && args.length == 3) {
            Player target = getServer().getPlayerExact(args[1]);
            int number = Integer.parseInt(args[2]);
            tryToGiveReward(target, number);
        } else {
            return false;
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!wrapDroppedItems.remove(event.getEntity().getLocation().getBlock())) return;
        if (easterEggItem == null) return;
        ItemStack item = event.getEntity().getItemStack();
        item = CustomPlugin.getInstance().getItemManager().wrapItemStack(item, CUSTOM_ID_EASTER_EGG);
        easterEggItem.getItemDescription().apply(item);
        event.getEntity().setItemStack(item);
    }

    ConfigurationSection getScores() {
        if (scores == null) {
            scores = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "scores.yml"));
        }
        return scores;
    }

    int getScore(Player player) {
        return getScores().getInt(player.getUniqueId().toString());
    }

    void setScore(Player player, int score) {
        getScores().set(player.getUniqueId().toString(), score);
    }

    int addScore(Player player, int score) {
        int result = getScore(player) + score;
        setScore(player, result);
        return result;
    }

    void saveScores() {
        if (scores == null) return;
        getDataFolder().mkdirs();
        try {
            scores.save(new File(getDataFolder(), "scores.yml"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    List<Head> getEasterEggHeads() {
        if (easterEggHeads == null) {
            easterEggHeads = new ArrayList<>();
            for (Map<?, ?> map: getConfig().getMapList("easter_eggs")) {
                Head head = Head.of(map);
                easterEggHeads.add(head);
            }
        }
        return easterEggHeads;
    }

    List<Head> getEasterBasketHeads() {
        if (easterBasketHeads == null) {
            easterBasketHeads = new ArrayList<>();
            for (Map<?, ?> map: getConfig().getMapList("easter_baskets")) {
                Head head = Head.of(map);
                easterBasketHeads.add(head);
            }
        }
        return easterBasketHeads;
    }

    Head getRandomEasterEggHead() {
        List<Head> list = getEasterEggHeads();
        return list.get(random.nextInt(list.size()));
    }

    Head getRandomEasterBasketHead() {
        List<Head> list = getEasterBasketHeads();
        return list.get(random.nextInt(list.size()));
    }

    void cleanupBlocks() {
        for (Iterator<Block> iter = easterBlocks.iterator(); iter.hasNext();) {
            if (iter.next().getType() != Material.SKULL) iter.remove();
        }
    }

    List<RewardItem> getRewardItems() {
        if (rewardItems == null) {
            rewardItems = new ArrayList<>();
            for (Map<?, ?> map: getConfig().getMapList("items")) {
                rewardItems.add(RewardItem.of(this, map));
            }
        }
        return rewardItems;
    }

    @SuppressWarnings("unchecked")
    Map<Integer, RewardItem> getRewards() {
        if (rewards == null) {
            rewards = new HashMap<>();
            for (Map<?, ?> map: getConfig().getMapList("rewards")) {
                rewards.put(((Number)map.get("Level")).intValue(), RewardItem.of(this, (Map<?, ?>)map.get("item")));
            }
        }
        return rewards;
    }

    void playJingle(Location loc) {
        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_HARP,
                                         0.2f + 0.1f * (float)i,
                                         0.5f + 0.2f * (float)i);
                i += 1;
                if (i > 7) cancel();
            }
        }.runTaskTimer(this, 0, 1);
    }

    void wrapDroppedItem(Block block) {
        wrapDroppedItems.add(block);
    }

    boolean notTooClose(Block block) {
        for (Block other: easterBlocks) {
            int dx = block.getX() - other.getX();
            int dy = block.getY() - other.getY();
            int dz = block.getZ() - other.getZ();
            if (dx * dx + dy * dy + dz * dz < 16 * 16) return false;
        }
        return true;
    }

    void spawnNewBlock(Block old) {
        for (int i = 0; i < 5; i += 1) {
            final int r = 32;
            int x = old.getX() + random.nextInt(r + r) - r;
            int z = old.getZ() + random.nextInt(r + r) - r;
            List<Block> possible = new ArrayList<>();
            int max = old.getWorld().getHighestBlockYAt(x, z) + 32;
            for (int y = 1; y <= max; y += 1) {
                Block block = old.getWorld().getBlockAt(x, y, z);
                if (block.getType() == Material.AIR
                    && block.getLightLevel() > 0
                    && block.getRelative(0, -1, 0).getType().isSolid()
                    && block.getRelative(0, -1, 0).getType() != Material.LEAVES
                    && block.getRelative(0, -1, 0).getType() != Material.LEAVES_2
                    && notTooClose(block)) {
                    possible.add(block);
                }
            }
            if (possible.isEmpty()) continue;
            Block block = possible.get(random.nextInt(possible.size()));
            String customId;
            if (random.nextInt(10) == 0) {
                customId = CUSTOM_ID_EASTER_BASKET;
            } else {
                customId = CUSTOM_ID_EASTER_EGG;
            }
            CustomPlugin.getInstance().getBlockManager().setBlock(block, customId);
            easterBlocks.add(block);
            getLogger().info("Spawned new " + customId + " at " + block.getX() + " " + block.getY() + " " + block.getZ());
        }
    }

    public boolean isEasterWorld(World bukkitWorld) {
        return bukkitWorld.getName().equals(this.world);
    }

    void rewardPlayerForEasterEggs(Player player, int eggs) {
        final int oldScore = getScore(player);
        final int newScore = addScore(player, eggs);
        saveScores();
        new BukkitRunnable() {
            int score = oldScore;
            @Override public void run() {
                score += 1;
                if (player.isValid()) {
                    player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 0.33f, 0.2f);
                    tryToGiveReward(player, score);
                }
                if (score >= newScore) {
                    cancel();
                }
            }
        }.runTaskTimer(this, 2, 2);
    }

    void tryToGiveReward(Player player, int score) {
        if (!giveNumberReward(player, score)
            && score > 0
            && score % 10 == 0) {
            giveRandomReward(player, score);
        }
    }

    boolean giveNumberReward(Player player, int score) {
        RewardItem reward = getRewards().get(score);
        if (reward == null) return false;
        reward.give(player, score);
        player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
        return true;
    }

    void giveRandomReward(Player player, int score) {
        player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
        RewardItem reward = getRewardItems().get(random.nextInt(getRewardItems().size()));
        reward.give(player, score);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!isEasterWorld(event.getBlock().getWorld())) return;
        if (CustomPlugin.getInstance().getBlockManager().getBlockWatcher(event.getBlock()) != null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEasterWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!isEasterWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerBucketEmpty(PlayerBucketFillEvent event) {
        if (!isEasterWorld(event.getBlockClicked().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerBucketFill(PlayerBucketEmptyEvent event) {
        if (!isEasterWorld(event.getBlockClicked().getWorld())) return;
        event.setCancelled(true);
    }
}
