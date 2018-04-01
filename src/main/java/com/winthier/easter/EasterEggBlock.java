package com.winthier.easter;

import com.winthier.custom.CustomPlugin;
import lombok.Getter;
import org.bukkit.entity.Item;

@Getter
public final class EasterEggBlock extends EasterBlock {
    private final String customId = EasterPlugin.CUSTOM_ID_EASTER_EGG;
    private final Type type = Type.EGG;

    EasterEggBlock(EasterPlugin plugin) {
        super(plugin);
    }
}
