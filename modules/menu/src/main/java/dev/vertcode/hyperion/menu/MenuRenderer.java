package dev.vertcode.hyperion.menu;

import org.bukkit.entity.Player;

/**
 * Draws menus with a menu library.
 *
 * <p>An implementation has a public constructor that takes the plugin, which the library is
 * set up with.
 */
public interface MenuRenderer {

    /**
     * Open a menu for a player. Call on the thread that owns the player.
     *
     * @param player the player
     * @param page the menu
     */
    void open(Player player, MenuPage page);
}
