package dev.vertcode.hyperion.api.model.behavior.builtin;

import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

/**
 * A bone that shows what the entity carrying the model holds in one hand.
 *
 * <p>The bone gets an item source as the model spawns, unless it already has one. While the hand
 * is empty the bone shows its own cubes. Give the bone another item source to take over.
 */
@ApiStatus.NonExtendable
public interface HandBehavior extends BoneBehavior {

    /**
     * Return the hand the bone shows.
     *
     * @return {@link EquipmentSlot#HAND} or {@link EquipmentSlot#OFF_HAND}
     */
    EquipmentSlot slot();
}
