package dev.vertcode.hyperion.config;

import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.EntityType;

/**
 * The {@code behaviors.seats.steering} section of {@code config.yml}: whether the player in a
 * model's first seat steers the entity carrying it, and how it moves.
 *
 * @param enabled whether riders steer by default
 * @param flying true to fly like a creative player, false to walk and jump like a horse
 * @param speed the speed in blocks per tick
 * @param jump the upward speed of a jump in blocks per tick, unused when flying
 * @param entities a choice per entity type, by its name such as {@code horse}, that wins over
 *     the default
 */
public record SeatSteering(boolean enabled, boolean flying, float speed, float jump, Map<String, Boolean> entities) {

    /**
     * Copy the choices, so the settings never change.
     */
    public SeatSteering {
        entities = Map.copyOf(entities);
    }

    /**
     * Return whether riders steer an entity of a type.
     *
     * @param type the entity's type
     * @return the choice for the type, or the default
     */
    public boolean enabledFor(final EntityType type) {
        return this.entities.getOrDefault(type.getKey().getKey().toLowerCase(Locale.ROOT), this.enabled);
    }
}
