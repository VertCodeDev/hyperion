package dev.vertcode.hyperion.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;

/**
 * What a hit or click on a hitbox's interaction reaches.
 */
public interface HitboxTarget {

    /**
     * Return the model the hitbox belongs to.
     *
     * @return the model
     */
    Model model();

    /**
     * Return the hitbox bone.
     *
     * @return the bone, or null for the box around the whole model
     */
    @Nullable Bone bone();

    /**
     * Return whether hits pass on to the entity carrying the model.
     *
     * @return true to hurt the entity
     */
    boolean forwardsDamage();

    /**
     * Return who sits in the model's seats. A hitbox is kept from them: from inside it, it would
     * catch every click they make.
     *
     * @return the riders' ids
     */
    default Set<UUID> riders() {
        Set<UUID> riders = new HashSet<>();
        for (SeatBehavior seat : this.model().seats()) {
            seat.passenger().ifPresent(rider -> riders.add(rider.getUniqueId()));
        }

        return riders;
    }
}
