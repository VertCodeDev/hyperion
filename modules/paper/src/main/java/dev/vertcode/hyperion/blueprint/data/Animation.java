package dev.vertcode.hyperion.blueprint.data;

import java.util.List;

/**
 * An animation from a Blockbench file, with its keyframes kept as saved.
 *
 * @param name the animation's name
 * @param loop what happens at the end
 * @param length the length in seconds
 * @param override whether it replaces other animations on the bones it moves, rather than
 *     adding to them
 * @param startDelay the delay before it starts, a number or Molang, in seconds
 * @param loopDelay the pause between loops, a number or Molang, in seconds
 * @param animators the bones and timeline it drives
 */
public record Animation(
        String name,
        LoopMode loop,
        float length,
        boolean override,
        String startDelay,
        String loopDelay,
        List<Animator> animators
) {

    /**
     * Copy the animators, so an animation never changes after it is read.
     */
    public Animation {
        animators = List.copyOf(animators);
    }
}
