package dev.vertcode.hyperion.menu;

import org.bukkit.entity.Player;

/**
 * What a button does when clicked. Runs on the thread that owns the player.
 */
@FunctionalInterface
public interface MenuAction {

    /**
     * Do nothing, for a button that only shows something.
     */
    MenuAction NONE = (player, click) -> true;

    /**
     * Handle a click.
     *
     * @param player who clicked
     * @param click how they clicked
     * @return true to keep the menu open and redraw every button, false to close it
     */
    boolean click(Player player, MenuClick click);
}
