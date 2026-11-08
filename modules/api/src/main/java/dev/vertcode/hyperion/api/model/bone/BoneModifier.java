package dev.vertcode.hyperion.api.model.bone;

/**
 * Changes a bone's transform from code every frame: look at a target, aim a turret, wag a tail by
 * a formula, or bend a leg to the ground.
 *
 * <p>A bone's transform is worked out in this order: its animation, then its
 * {@link BoneAppearance#transform()}, then its modifiers in the order they were added. Children
 * follow. Called on the region that owns the model, once per frame while it has viewers, so keep
 * it cheap. Hyperion drops a modifier when the plugin it comes from disables.
 *
 * <pre>{@code
 * bone.modifier(Key.key("myplugin", "spin"), (_, transform) ->
 *         transform.withRotation(new Quaternionf().rotateY(System.nanoTime() / 1.0e9f)));
 * }</pre>
 */
@FunctionalInterface
public interface BoneModifier {

    /**
     * Change the bone's transform for this frame.
     *
     * @param bone the bone being moved
     * @param transform the bone's transform so far, relative to its parent
     * @return the transform to use
     */
    BoneTransform modify(Bone bone, BoneTransform transform);
}
