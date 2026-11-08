package dev.vertcode.hyperion.blueprint.data;

import java.util.List;

/**
 * The keyframes of one animation for one bone, or for the effects timeline.
 *
 * @param target the id of the group it drives, or {@code effects} for the timeline
 * @param name the name Blockbench showed for it, the bone's name for a bone
 * @param type what it drives
 * @param keyframes the keyframes, sorted by time
 */
public record Animator(String target, String name, AnimatorType type, List<Keyframe> keyframes) {

    /**
     * Copy the keyframes, so an animator never changes after it is read.
     */
    public Animator {
        keyframes = List.copyOf(keyframes);
    }
}
