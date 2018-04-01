package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.custom.util.Dirty;
import com.winthier.custom.util.Msg;
import com.winthier.playercache.PlayerCache;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

@Getter
public final class EasterPlugin extends JavaPlugin implements Listener {
    static final String CUSTOM_ID_EASTER_EGG = "easter:easter_egg";
    static final String CUSTOM_ID_EASTER_BASKET = "easter:easter_basket";
    private List<Head> easterEggHeads;
    private List<Head> easterBasketHeads;
    private final Random random = new Random(System.currentTimeMillis());
    private String world;
    private EasterEggItem easterEggItem;
    private YamlConfiguration scores;
    private List<RewardItem> rewardItems;
    private Map<Integer, RewardItem> rewards;
    final Set<Block> easterBlocks = new HashSet<>();
    boolean easterEnabled = false;
    private Scoreboard scoreboard = null;
    private Objective scoreboardObjective = null;
    private final Map<UUID, Integer> scoreboardTimer = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        new BukkitRunnable() {
            @Override public void run() {
                on20Ticks();
            }
        }.runTaskTimer(this, 20, 20);
    }

    @Override
    public void onDisable() {
        for (Player player: getServer().getOnlinePlayers()) {
            player.removeMetadata("MiniMapCursors", this);
        }
    }

    void on20Ticks() {
        if (!easterEnabled) return;
        World eventWorld = getServer().getWorld(world);
        if (eventWorld == null) return;
        for (Player player: getServer().getOnlinePlayers()) {
            Block nearestBasket = null;
            boolean isInEventWorld = player.getWorld().equals(eventWorld);
            if (isInEventWorld) {
                final Location loc = player.getLocation();
                int x = loc.getBlockX();
                int z = loc.getBlockZ();
                // Spawn new basket?
                int mindx = Integer.MAX_VALUE;
                int mindz = Integer.MAX_VALUE;
                for (Block easterBlock: easterBlocks) {
                    int dx = Math.abs(x - easterBlock.getX());
                    int dz = Math.abs(z - easterBlock.getZ());
                    if (dx < mindx && dz < mindz) {
                        mindx = dx;
                        mindz = dz;
                        nearestBasket = easterBlock;
                    }
                }
                if (mindx > 64 || mindz > 64) {
                    nearestBasket = spawnEasterBasket(loc);
                }
            }
            // Update Mini Map
            List<Map> cursorList = new ArrayList<>();
            if (isInEventWorld && nearestBasket != null) {
                Map<String, Object> cursorMap = new HashMap<>();
                cursorMap.put("block", nearestBasket);
                cursorMap.put("type", MapCursor.Type.WHITE_CROSS);
                cursorList.add(cursorMap);
            }
            player.setMetadata("MiniMapCursors", new FixedMetadataValue(this, cursorList));
            // Tick timer
            if (scoreboard != null) {
                Integer timer = scoreboardTimer.remove(player.getUniqueId());
                if (timer != null && timer > 0) {
                    int newTimer = timer - 1;
                    if (newTimer > 0) {
                        scoreboardTimer.put(player.getUniqueId(), newTimer);
                    } else {
                        player.setScoreboard(getServer().getScoreboardManager().getMainScoreboard());
                    }
                }
            }
        }
        easterBlocks.clear();
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        reloadConfig();
        easterBlocks.clear();
        scores = null;
        rewardItems = null;
        rewards = null;
        world = getConfig().getString("World");
        event.addBlock(new EasterEggBlock(this));
        event.addBlock(new EasterBasketBlock(this));
        easterEggItem = new EasterEggItem(this);
        event.addItem(easterEggItem);
        easterEnabled = getConfig().getBoolean("Enabled");
        if (easterEnabled) getLogger().info("Easter enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = args.length == 0 ? null : args[0].toLowerCase();
        if (cmd == null) {
            return false;
        } else if (cmd.equals("info") && args.length == 2) {
            Player target = getServer().getPlayerExact(args[1]);
            target.sendMessage("So far, you have returned " + ChatColor.GREEN + getScore(target) + ChatColor.RESET + " easter eggs.");
        } else if (cmd.equals("config") && args.length == 1) {
            saveDefaultConfig();
            sender.sendMessage("Saved default config");
        } else if (cmd.equals("reward") && args.length == 3) {
            Player target = getServer().getPlayerExact(args[1]);
            int number = Integer.parseInt(args[2]);
            giveRandomReward(target);
        } else if (cmd.equals("hi") || cmd.equals("highscore")) {
            class Hi {
                int score; UUID uuid; String name;
            }
            List<Hi> list = new ArrayList<>();
            for (String key: getScores().getKeys(false)) {
                Hi hi = new Hi();
                hi.score = getScores().getInt(key);
                UUID uuid = UUID.fromString(key);
                hi.uuid = uuid;
                hi.name = PlayerCache.nameForUuid(uuid);
                list.add(hi);
            }
            sender.sendMessage("-- Easter Highscore (" + list.size() + ")");
            Collections.sort(list, (a, b) -> Integer.compare(b.score, a.score));
            int rank = 1;
            for (Hi hi: list) {
                sender.sendMessage("" + rank++ + ") " + hi.score + " " + hi.name + " " + hi.uuid);
            }
        } else {
            return false;
        }
        return true;
    }

    ConfigurationSection getScores() {
        if (scores == null) {
            scores = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "scores.yml"));
            scoreboard = getServer().getScoreboardManager().getNewScoreboard();
            scoreboardObjective = scoreboard.registerNewObjective("high", "dummy");
            scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            scoreboardObjective.setDisplayName(Msg.format("&aEaster Highscore"));
            for (String key: scores.getKeys(false)) {
                int value = scores.getInt(key);
                String name = PlayerCache.nameForUuid(UUID.fromString(key));
                if (name != null) {
                    scoreboardObjective.getScore(name).setScore(value);
                }
            }
        }
        return scores;
    }

    int getScore(Player player) {
        return getScores().getInt(player.getUniqueId().toString());
    }

    void setScore(Player player, int score) {
        getScores().set(player.getUniqueId().toString(), score);
        saveScores();
        if (scoreboardObjective != null) {
            scoreboardObjective.getScore(player.getName()).setScore(score);
        }
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

    boolean notTooClose(Block block) {
        for (Block other: easterBlocks) {
            int dx = block.getX() - other.getX();
            int dy = block.getY() - other.getY();
            int dz = block.getZ() - other.getZ();
            if (dx * dx + dy * dy + dz * dz < 16 * 16) return false;
        }
        return true;
    }

    Block spawnEasterBasket(Location loc) {
        if (!isEasterWorld(loc.getWorld())) return null;
        final int r = 64;
        int x = loc.getBlockX() + random.nextInt(r + r) - r;
        int z = loc.getBlockZ() + random.nextInt(r + r) - r;
        List<Block> possible = new ArrayList<>();
        int max = loc.getWorld().getHighestBlockYAt(x, z) + 32;
        for (int y = 1; y <= max; y += 1) {
            Block block = loc.getWorld().getBlockAt(x, y, z);
            if (block.getType() == Material.AIR
                && block.getLightFromSky() > 0
                && block.getRelative(0, -1, 0).getType().isSolid()
                && block.getRelative(0, -1, 0).getType() != Material.LEAVES
                && block.getRelative(0, -1, 0).getType() != Material.LEAVES_2) {
                possible.add(block);
            }
        }
        if (possible.isEmpty()) return null;
        Block block = possible.get(random.nextInt(possible.size()));
        final String customId = CUSTOM_ID_EASTER_BASKET;
        CustomPlugin.getInstance().getBlockManager().setBlock(block, customId);
        easterBlocks.add(block);
        getLogger().info("Spawned new " + customId + " at " + block.getX() + " " + block.getY() + " " + block.getZ());
        return block;
    }

    public boolean isEasterWorld(World bukkitWorld) {
        return bukkitWorld.getName().equals(this.world);
    }

    boolean giveNumberReward(Player player, int score) {
        RewardItem reward = getRewards().get(score);
        if (reward == null) return false;
        reward.give(player, score);
        player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
        return true;
    }

    void giveRandomReward(Player player) {
        player.playSound(player.getEyeLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.5f, 2.0f);
        RewardItem reward = getRewardItems().get(random.nextInt(getRewardItems().size()));
        reward.give(player);
    }

    ItemStack createDumbEasterEggItem(int amount) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, amount, (short)3);
        Head head = getRandomEasterEggHead();
        item = Dirty.setSkullOwner(item, head.getName(), head.getId(), head.getTexture());
        return item;
    }

    void basketFound(Player player, Location loc) {
        getLogger().info(player.getName() + " found Easter Bastet at + " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
        addScore(player, 1);
        if (scoreboard != null) {
            player.setScoreboard(scoreboard);
            scoreboardTimer.put(player.getUniqueId(), 10);
        }
        playJingle(loc);
        Item item = loc.getWorld().dropItem(loc, createDumbEasterEggItem(1));
        item.setVelocity(new Vector(getRandom().nextDouble() * 1.0 - 0.5,
                                    getRandom().nextDouble() * 1.0,
                                    getRandom().nextDouble() * 1.0 - 0.5));
        int rand = getRandom().nextInt(100);
        if (rand < 5) {
            giveRandomReward(player);
        } else if (rand < 50) {
            switch (getRandom().nextInt(50)) {
            case 0: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.CREEPER); break;
            case 1: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE); break;
            case 2: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.HUSK); break;
            case 3: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.SKELETON); break;
            case 4: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.STRAY); break;
            case 5: for (int i = 0; i < 5; i += 1) loc.getWorld().spawnEntity(loc, EntityType.SLIME); break;
            case 6: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER); break;
            case 7: for (int i = 0; i < 5; i += 1) loc.getWorld().spawnEntity(loc, EntityType.PARROT); break;
            case 8: for (int i = 0; i < 5; i += 1) loc.getWorld().spawnEntity(loc, EntityType.CHICKEN); break;
            case 9: for (int i = 0; i < 2; i += 1) loc.getWorld().spawnEntity(loc, EntityType.POLAR_BEAR); break;
            case 10: for (int i = 0; i < 2; i += 1) loc.getWorld().spawnEntity(loc, EntityType.LLAMA); break;
            case 11: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.OCELOT); break;
            case 12: for (int i = 0; i < 3; i += 1) loc.getWorld().spawnEntity(loc, EntityType.RABBIT); break;
            case 13: loc.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT); break;
            case 14: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DIAMOND, 1)); break;
            case 15: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLD_INGOT, 1)); break;
            case 16: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.IRON_INGOT, 1)); break;
            case 17: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.STICK, 1)); break;
            case 18: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.EMERALD, 1)); break;
            case 19: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.DRAGONS_BREATH, 1)); break;
            case 20: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.BRICK, 1)); break;
            case 21: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLDEN_APPLE, 1)); break;
            case 22: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLDEN_CARROT, 1)); break;
            case 23: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.CARROT_ITEM, 15)); break;
            case 24: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.BONE, 1)); break;
            case 25: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.MILK_BUCKET, 1)); break;
            case 26: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.TOTEM, 1)); break;
            case 27: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GOLD_RECORD, 1)); break;
            case 28: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.GREEN_RECORD, 1)); break;
            case 29: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_10, 1)); break;
            case 30: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_11, 1)); break;
            case 31: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_12, 1)); break;
            case 32: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_3, 1)); break;
            case 33: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_4, 1)); break;
            case 34: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_5, 1)); break;
            case 35: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_6, 1)); break;
            case 36: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_7, 1)); break;
            case 37: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_8, 1)); break;
            case 38: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.RECORD_9, 1)); break;
            default: loc.getWorld().dropItemNaturally(loc, new ItemStack(Material.COAL, 1)); break;
            }
        } else {
            for (int i = 0; i < 10; i += 1) {
                switch (getRandom().nextInt(3)) {
                case 1:
                    Rabbit rabbit = loc.getWorld().spawn(loc, Rabbit.class, new Consumer<Rabbit>() {
                        @Override public void accept(Rabbit rabbit) {
                            Rabbit.Type type = Rabbit.Type.values()[getRandom().nextInt(Rabbit.Type.values().length)];
                            rabbit.setRabbitType(type);
                            rabbit.setRemoveWhenFarAway(true);
                            rabbit.setVelocity(new Vector(getRandom().nextDouble() * 1.0 - 0.5,
                                                          getRandom().nextDouble() * 0.5,
                                                          getRandom().nextDouble() * 1.0 - 0.5));
                        }
                    });
                    break;
                case 2:
                    Sheep sheep = loc.getWorld().spawn(loc, Sheep.class, new Consumer<Sheep>() {
                        @Override public void accept(Sheep sheep) {
                            DyeColor type = DyeColor.values()[getRandom().nextInt(DyeColor.values().length)];
                            sheep.setColor(type);
                            sheep.setBaby();
                            sheep.setRemoveWhenFarAway(true);
                            sheep.setVelocity(new Vector(getRandom().nextDouble() * 1.0 - 0.5,
                                                         getRandom().nextDouble() * 0.5,
                                                         getRandom().nextDouble() * 1.0 - 0.5));
                        }
                    });
                default:
                    break;
                }
            }
        }
    }
}
