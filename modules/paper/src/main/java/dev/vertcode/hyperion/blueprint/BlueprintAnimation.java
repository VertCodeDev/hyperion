package dev.vertcode.hyperion.blueprint;

import java.util.LinkedHashSet;
import java.util.Set;

import dev.vertcode.hyperion.api.animation.AnimationBlueprint;
import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.Animator;
import dev.vertcode.hyperion.blueprint.data.AnimatorType;

/**
 * An animation of a loaded blueprint as the api shows it.
 *
 * @param name the animation's name
 * @param length the length in seconds
 * @param loop what it does at its end
 * @param bones the names of the bones it moves
 */
public record BlueprintAnimation(String name, float length, LoopMode loop, Set<String> bones) implements AnimationBlueprint {

    /**
     * Copy the bones, so an animation never changes after it is built.
     */
    public BlueprintAnimation {
        bones = Set.copyOf(bones);
    }

    /**
     * Read the api's view of an animation from the file's.
     *
     * @param animation the animation as read
     * @return the api's view
     */
    public static BlueprintAnimation of(final Animation animation) {
        Set<String> bones = new LinkedHashSet<>();
        for (Animator animator : animation.animators()) {
            if (animator.type() == AnimatorType.BONE) {
                bones.add(animator.name());
            }
        }

        return new BlueprintAnimation(animation.name(), animation.length(), LoopMode.valueOf(animation.loop().name()), bones);
    }
}
