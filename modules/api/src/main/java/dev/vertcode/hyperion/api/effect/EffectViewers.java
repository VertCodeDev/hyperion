package dev.vertcode.hyperion.api.effect;

import java.util.UUID;

/**
 * Who sees and hears an effect.
 */
public sealed interface EffectViewers {

    /**
     * The players who see the model the effect is anchored to. On anything else, the players
     * within {@code effects.view-distance} of {@code config.yml}.
     *
     * @return the viewers
     */
    static EffectViewers model() {
        return Model.INSTANCE;
    }

    /**
     * The players within a radius of where the effect starts, checked every tick.
     *
     * @param radius the radius in blocks
     * @return the viewers
     * @throws IllegalArgumentException if the radius is not above 0
     */
    static EffectViewers nearby(final double radius) {
        return new Nearby(radius);
    }

    /**
     * Every player in the effect's world.
     *
     * @return the viewers
     */
    static EffectViewers everyone() {
        return Everyone.INSTANCE;
    }

    /**
     * One player only, such as the player a model's view plays for.
     *
     * @param player the player's id
     * @return the viewers
     */
    static EffectViewers only(final UUID player) {
        return new Only(player);
    }

    /**
     * The players who see the model.
     */
    enum Model implements EffectViewers {

        /**
         * The only value.
         */
        INSTANCE
    }

    /**
     * Every player in the world.
     */
    enum Everyone implements EffectViewers {

        /**
         * The only value.
         */
        INSTANCE
    }

    /**
     * The players within a radius.
     *
     * @param radius the radius in blocks
     */
    record Nearby(double radius) implements EffectViewers {

        /**
         * Check the radius.
         *
         * @throws IllegalArgumentException if the radius is not above 0
         */
        public Nearby {
            if (!(radius > 0d)) {
                throw new IllegalArgumentException("A viewer radius must be above 0, got " + radius);
            }
        }
    }

    /**
     * One player.
     *
     * @param player the player's id
     */
    record Only(UUID player) implements EffectViewers {
    }
}
