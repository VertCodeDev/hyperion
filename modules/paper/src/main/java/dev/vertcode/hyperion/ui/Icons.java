package dev.vertcode.hyperion.ui;

import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Build the items menu buttons show: a material, a name and lore lines, without the italics
 * Minecraft puts on custom item names.
 */
final class Icons {

    private Icons() {
    }

    /**
     * Build an icon.
     *
     * @param material the item
     * @param name the name
     * @param lore the lines under the name
     * @return the icon
     */
    static ItemStack of(final Material material, final Component name, final Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name));
        List<Component> lines = new ArrayList<>(lore.length);
        for (Component line : lore) {
            lines.add(plain(line));
        }

        meta.lore(lines);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Turn the italics off, which Minecraft turns on for custom names and lore.
     *
     * @param text the text
     * @return the text without italics
     */
    private static Component plain(final Component text) {
        return text.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
