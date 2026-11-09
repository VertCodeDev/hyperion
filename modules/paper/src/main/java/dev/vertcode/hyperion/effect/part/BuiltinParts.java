package dev.vertcode.hyperion.effect.part;

import java.util.List;
import java.util.function.Function;

import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectPart;
import dev.vertcode.hyperion.api.effect.EffectPartType;

/**
 * The part types Hyperion ships: {@code particle}, {@code sound}, {@code burst} and
 * {@code event}.
 */
public final class BuiltinParts {

    private BuiltinParts() {
    }

    /**
     * Return every built-in part type.
     *
     * @return the types
     */
    public static List<EffectPartType> all() {
        return List.of(
                type(EffectPart.PARTICLE, true, ParticleActor::new),
                type(EffectPart.SOUND, false, SoundActor::new),
                type(EffectPart.BURST, false, BurstActor::new),
                type(EffectPart.EVENT, false, EventActor::new)
        );
    }

    /**
     * Make a part type from its key and its actor.
     *
     * @param key the type's key
     * @param sampled whether it acts every tick
     * @param actor creates the actor for one part
     * @return the type
     */
    private static EffectPartType type(final Key key, final boolean sampled, final Function<EffectContext, EffectActor> actor) {
        return new EffectPartType() {

            @Override
            public Key key() {
                return key;
            }

            @Override
            public boolean sampled() {
                return sampled;
            }

            @Override
            public EffectActor create(final EffectPart part, final EffectContext context) {
                return actor.apply(context);
            }
        };
    }
}
