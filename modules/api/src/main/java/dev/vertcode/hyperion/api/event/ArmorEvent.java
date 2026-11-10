package dev.vertcode.hyperion.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

import dev.vertcode.hyperion.api.armor.ArmorSet;

/**
 * An event about a player and one Hyperion armor set, fired on the player's own thread.
 */
public abstract class ArmorEvent extends PlayerEvent {

    private final ArmorSet set;

    /**
     * Create an event about a player and a set.
     *
     * @param player the player
     * @param set the set
     */
    protected ArmorEvent(final Player player, final ArmorSet set) {
        super(player);
        this.set = set;
    }

    /**
     * Return the set this event is about.
     *
     * @return the set
     */
    public ArmorSet set() {
        return this.set;
    }
}
