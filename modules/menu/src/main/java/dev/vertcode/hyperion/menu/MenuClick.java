package dev.vertcode.hyperion.menu;

import org.bukkit.event.inventory.ClickType;

/**
 * How a player clicked a button.
 */
public enum MenuClick {

    /**
     * A plain left click.
     */
    LEFT,

    /**
     * A plain right click.
     */
    RIGHT,

    /**
     * A left click while sneaking.
     */
    SHIFT_LEFT,

    /**
     * A right click while sneaking.
     */
    SHIFT_RIGHT,

    /**
     * Any other click, such as a number key or a drop.
     */
    OTHER;

    /**
     * Read Bukkit's click type.
     *
     * @param type the click type
     * @return the click
     */
    public static MenuClick of(final ClickType type) {
        return switch (type) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case SHIFT_LEFT -> SHIFT_LEFT;
            case SHIFT_RIGHT -> SHIFT_RIGHT;
            default -> OTHER;
        };
    }
}
