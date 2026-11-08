package dev.vertcode.hyperion.api.model.bone;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionfc;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

/**
 * One bone of a live model.
 *
 * <p>Change its look with the {@link BoneAppearance} setters from any thread. Ask it for its
 * behaviors to reach a seat, a hitbox or anything another plugin attached:
 *
 * <pre>{@code
 * model.bone("saddle")
 *         .flatMap(bone -> bone.behavior(SeatBehavior.class))
 *         .ifPresent(seat -> seat.mount(player));
 * }</pre>
 */
@ApiStatus.NonExtendable
public interface Bone extends BoneAppearance<Bone> {

    /**
     * Return the bone's blueprint.
     *
     * @return the blueprint
     */
    BoneBlueprint blueprint();

    /**
     * Return the model this bone belongs to.
     *
     * @return the model
     */
    Model model();

    /**
     * Return the bone's name. Same as {@code blueprint().name()}.
     *
     * @return the name
     */
    String name();

    /**
     * Return the bone this one hangs from.
     *
     * @return the parent, or empty for a root bone
     */
    Optional<Bone> parent();

    /**
     * Return the bones that hang from this one.
     *
     * @return the children
     */
    @Unmodifiable List<Bone> children();

    /**
     * Return where the bone's pivot is in the world, animation included, as of the last frame.
     *
     * @return a copy of the location
     */
    Location location();

    /**
     * Return the bone's rotation in the world, animation included, as of the last frame.
     *
     * @return the world rotation
     */
    Quaternionfc rotation();

    /**
     * Return every behavior attached to this bone.
     *
     * @return the behaviors
     */
    @Unmodifiable Collection<BoneBehavior> behaviors();

    /**
     * Find a behavior of a given type on this bone.
     *
     * @param type the behavior interface or class, such as {@code SeatBehavior.class}
     * @param <B> the behavior type
     * @return the behavior, or empty if the bone has none of that type
     */
    <B extends BoneBehavior> Optional<B> behavior(Class<B> type);
}
