package dev.vertcode.hyperion.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.vertcode.hyperion.blueprint.data.Animation;

/**
 * What changed between two loads of the same model, for the reload report.
 *
 * @param addedBones bones the new load has and the old one did not
 * @param removedBones bones the old load had and the new one does not
 * @param addedAnimations animations the new load has and the old one did not
 * @param removedAnimations animations the old load had and the new one does not
 * @param changedAnimations animations in both loads whose keyframes or settings differ
 */
public record BlueprintChanges(
        List<String> addedBones,
        List<String> removedBones,
        List<String> addedAnimations,
        List<String> removedAnimations,
        List<String> changedAnimations
) {

    /**
     * Copy every list, so the changes never change.
     */
    public BlueprintChanges {
        addedBones = List.copyOf(addedBones);
        removedBones = List.copyOf(removedBones);
        addedAnimations = List.copyOf(addedAnimations);
        removedAnimations = List.copyOf(removedAnimations);
        changedAnimations = List.copyOf(changedAnimations);
    }

    /**
     * Compare two loads of the same model.
     *
     * @param before the old load
     * @param after the new load
     * @return what changed
     */
    public static BlueprintChanges between(final Blueprint before, final Blueprint after) {
        List<String> beforeBones = boneNames(before);
        List<String> afterBones = boneNames(after);
        Set<String> beforeAnimations = before.animations();
        Set<String> afterAnimations = after.animations();

        List<String> changed = new ArrayList<>();
        for (String name : afterAnimations) {
            Optional<Animation> old = before.animation(name);
            if (old.isPresent() && !old.equals(after.animation(name))) {
                changed.add(name);
            }
        }

        return new BlueprintChanges(
                missingFrom(afterBones, beforeBones),
                missingFrom(beforeBones, afterBones),
                missingFrom(List.copyOf(afterAnimations), List.copyOf(beforeAnimations)),
                missingFrom(List.copyOf(beforeAnimations), List.copyOf(afterAnimations)),
                changed
        );
    }

    /**
     * Return whether nothing Hyperion tracks changed.
     *
     * @return true when both loads have the same bones and animations
     */
    public boolean isEmpty() {
        return this.addedBones.isEmpty()
                && this.removedBones.isEmpty()
                && this.addedAnimations.isEmpty()
                && this.removedAnimations.isEmpty()
                && this.changedAnimations.isEmpty();
    }

    /**
     * Describe the changes in one short line, such as {@code +2 bones, walk changed}.
     *
     * @return the summary, or {@code no changes}
     */
    public String describe() {
        List<String> parts = new ArrayList<>();
        count(parts, "+", this.addedBones.size(), "bone");
        count(parts, "-", this.removedBones.size(), "bone");
        count(parts, "+", this.addedAnimations.size(), "animation");
        count(parts, "-", this.removedAnimations.size(), "animation");
        if (!this.changedAnimations.isEmpty()) {
            parts.add(String.join(", ", this.changedAnimations) + " changed");
        }

        return parts.isEmpty() ? "no changes" : String.join(", ", parts);
    }

    /**
     * Return the names of every bone of a blueprint.
     *
     * @param blueprint the blueprint
     * @return the bone names, parents first
     */
    private static List<String> boneNames(final Blueprint blueprint) {
        List<String> names = new ArrayList<>();
        for (BlueprintBone bone : blueprint.bones()) {
            names.add(bone.name());
        }

        return names;
    }

    /**
     * Return the names in one list that the other lacks.
     *
     * @param names the names to check
     * @param other the names to check against
     * @return the names missing from the other list, in order
     */
    private static List<String> missingFrom(final List<String> names, final List<String> other) {
        List<String> missing = new ArrayList<>();
        for (String name : names) {
            if (!other.contains(name)) {
                missing.add(name);
            }
        }

        return missing;
    }

    /**
     * Add a count such as {@code +2 bones} when it is not 0.
     *
     * @param parts the parts of the summary
     * @param sign {@code +} or {@code -}
     * @param count how many
     * @param noun what is counted, singular
     */
    private static void count(final List<String> parts, final String sign, final int count, final String noun) {
        if (count > 0) {
            parts.add(sign + count + " " + noun + (count == 1 ? "" : "s"));
        }
    }
}
