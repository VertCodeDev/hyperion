package dev.vertcode.hyperion.api.event;

/**
 * Why a player's Hyperion armor changed.
 */
public enum ArmorChangeCause {

    /**
     * The player put it on or took it off: a right-click, or a click in their inventory.
     */
    PLAYER,

    /**
     * A dispenser put it on the player.
     */
    DISPENSER,

    /**
     * It broke.
     */
    BREAK,

    /**
     * The player died and dropped it.
     */
    DEATH,

    /**
     * The player joined wearing it, so a set bonus can switch on.
     */
    JOIN,

    /**
     * The player left wearing it, so a set bonus can be cleaned up.
     */
    QUIT,

    /**
     * Anything else: a command, another plugin, or a reload that removed the set.
     */
    OTHER
}
