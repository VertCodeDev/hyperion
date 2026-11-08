package dev.vertcode.hyperion.api.model.bone;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.kyori.adventure.key.Key;
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
     * Return every bone below this one: children, their children and so on, parents first.
     *
     * @return the descendants, not including this bone
     */
    @Unmodifiable List<Bone> descendants();

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
     * Add a modifier that changes this bone's transform every frame, or replace the one with the
     * same key. Safe from any thread; it starts on the next frame.
     *
     * @param key the modifier's key, in your plugin's namespace
     * @param modifier the modifier
     * @return this bone
     */
    Bone modifier(Key key, BoneModifier modifier);

    /**
     * Remove a modifier. Safe from any thread.
     *
     * @param key the modifier's key
     * @return whether a modifier was removed
     */
    boolean removeModifier(Key key);

    /**
     * Return the keys of this bone's modifiers, in the order they run.
     *
     * @return the modifier keys
     */
    @Unmodifiable List<Key> modifiers();

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
