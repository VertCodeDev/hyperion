package dev.vertcode.hyperion.api.model.behavior;

import java.util.function.Function;

import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.model.bone.Bone;

/**
 * The behavior type behind {@link BehaviorType#of}.
 *
 * @param key the type's key
 * @param behaviorClass the class of behavior it creates
 * @param matcher which bones get it
 * @param factory creates the behavior for one live bone
 * @param <B> the behavior type
 */
record SimpleBehaviorType<B extends BoneBehavior>(
        Key key,
        Class<B> behaviorClass,
        BehaviorMatcher matcher,
        Function<Bone, B> factory
) implements BehaviorType<B> {

    @Override
    public B create(final Bone bone) {
        return this.factory.apply(bone);
    }
}
