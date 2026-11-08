package dev.vertcode.hyperion.config;

import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.EntityType;

/**
 * The {@code models.auto-animations} section of {@code config.yml}: whether models on entities
 * play their idle and walk animations by themselves, and what those are called.
 *
 * @param enabled whether they do by default
 * @param idle the name of the animation played while the entity stands
 * @param walk the name of the animation played while it moves
 * @param entities a choice per entity type, by its name such as {@code armor_stand}, that wins
 *     over the default
 */
public record AutoAnimations(boolean enabled, String idle, String walk, Map<String, Boolean> entities) {

    /**
     * Copy the choices, so the settings never change.
     */
    public AutoAnimations {
        entities = Map.copyOf(entities);
    }

    /**
     * Return whether models on an entity of a type play the animations by themselves.
     *
     * @param type the entity's type
     * @return the choice for the type, or the default
     */
    public boolean enabledFor(final EntityType type) {
        return this.entities.getOrDefault(type.getKey().getKey().toLowerCase(Locale.ROOT), this.enabled);
    }
}
