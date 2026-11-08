package dev.vertcode.hyperion.api.model.behavior;

import java.util.function.Function;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;

import dev.vertcode.hyperion.api.model.bone.Bone;

/**
 * A kind of behavior a bone can have, and how to tell which bones have it.
 *
 * <p>When a model spawns, every registered type is asked whether it matches each bone, and
 * creates one behavior for every bone it matches. Most types are one call:
 *
 * <pre>{@code
 * Hyperion.behaviors().register(this, BehaviorType.of(
 *         Key.key("myplugin", "flame"),
 *         FlameBehavior.class,
 *         BehaviorMatcher.namePrefix("flame_").or(BehaviorMatcher.tagged("myplugin:flame")),
 *         FlameBehavior::new
 * ));
 * }</pre>
 *
 * @param <B> the behavior this type creates
 */
public interface BehaviorType<B extends BoneBehavior> extends Keyed {

    /**
     * Create a behavior type from its parts.
     *
     * @param key the type's key
     * @param behaviorClass the class of behavior it creates
     * @param matcher which bones get it
     * @param factory creates the behavior for one live bone
     * @param <B> the behavior type
     * @return the behavior type
     */
    static <B extends BoneBehavior> BehaviorType<B> of(
            final Key key,
            final Class<B> behaviorClass,
            final BehaviorMatcher matcher,
            final Function<Bone, B> factory
    ) {
        return new SimpleBehaviorType<>(key, behaviorClass, matcher, factory);
    }

    /**
     * Return the class of behavior this type creates, used by {@link Bone#behavior(Class)}.
     *
     * @return the behavior class
     */
    Class<B> behaviorClass();

    /**
     * Return the rule that decides which bones get this behavior.
     *
     * @return the matcher
     */
    BehaviorMatcher matcher();

    /**
     * Create the behavior for one live bone. Called on the model's region as it spawns.
     *
     * @param bone the bone the behavior belongs to
     * @return the new behavior
     */
    B create(Bone bone);
}
