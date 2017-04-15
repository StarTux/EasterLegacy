package com.winthier.easter;

import com.winthier.custom.util.Msg;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Getter @RequiredArgsConstructor
final class RewardItem {
    private final EasterPlugin plugin;
    private final String name, description;
    private final List<String> commands;
    private final ItemStack itemStack;

    @SuppressWarnings("unchecked")
    static RewardItem of(EasterPlugin plugin, Map<?, ?> map) {
        String name = (String)map.get("Name");
        String description = (String)map.get("Description");
        List<String> commands = (List<String>)map.get("Commands");
        ItemStack itemStack = (ItemStack)map.get("item");
        return new RewardItem(plugin, name, description, commands, itemStack);
    }

    void give(Player player, int score) {
        plugin.getLogger().info("Giving reward '" + name + "' to " + player.getName() + "...");
        Msg.send(player, "&a%d&r Easter Eggs! You win: &a%s", score, description);
        if (commands != null) {
            for (String string: commands) {
                Msg.consoleCommand(string.replace("%player%", player.getName()));
            }
        }
        if (itemStack != null) {
            player.getWorld().dropItem(player.getEyeLocation(), itemStack.clone()).setPickupDelay(0);
        }
    }
}
