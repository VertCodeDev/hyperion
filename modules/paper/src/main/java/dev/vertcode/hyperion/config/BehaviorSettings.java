package dev.vertcode.hyperion.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.model.hitbox.HitboxMode;

/**
 * The {@code behaviors} section of {@code config.yml}: which bone names get which built-in
 * behavior, and how those behave.
 *
 * @param prefixes the name prefixes per built-in behavior, by the behavior's key, lower case
 * @param headMaxYaw how far a head turns left or right from the body, in degrees
 * @param headMaxPitch how far a head looks up or down, in degrees
 * @param hitboxMode how players hit and click models
 * @param forwardDamage whether hits on a model on an entity hurt the entity
 * @param clickToRide whether right-clicking a model with a free seat seats the player
 * @param steering whether the rider of the first seat steers, and how
 */
public record BehaviorSettings(
        Map<Key, List<String>> prefixes,
        float headMaxYaw,
        float headMaxPitch,
        HitboxMode hitboxMode,
        boolean forwardDamage,
        boolean clickToRide,
        SeatSteering steering
) {

    /**
     * Copy the prefixes, so the settings never change.
     */
    public BehaviorSettings {
        prefixes = Map.copyOf(prefixes);
    }

    /**
     * Return whether a bone name starts with one of a behavior's prefixes, ignoring case.
     *
     * @param behavior the behavior's key
     * @param name the bone's name
     * @return true when the bone gets the behavior by its name
     */
    public boolean matches(final Key behavior, final String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        for (String prefix : this.prefixes.getOrDefault(behavior, List.of())) {
            if (lowered.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}
