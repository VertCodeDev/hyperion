package dev.vertcode.hyperion.api.model.bone;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Appearance;

/**
 * The look of one bone: everything a model has, plus an extra transform and an item.
 *
 * @param <S> the type setters return, so a chain keeps the concrete type
 */
@ApiStatus.NonExtendable
public interface BoneAppearance<S extends BoneAppearance<S>> extends Appearance<S> {

    /**
     * Return the extra transform applied on top of the animation.
     *
     * @return the transform, identity by default
     */
    BoneTransform transform();

    /**
     * Offset, turn or stretch this bone on top of whatever the animation does. Children follow.
     *
     * @param transform the extra transform; {@link BoneTransform#IDENTITY} to clear it
     * @return this object
     */
    S transform(BoneTransform transform);

    /**
     * Return the item shown in place of the bone's own cubes, as of the last frame. For a
     * per-viewer source this is null; ask the viewer's {@code PlayerView} instead.
     *
     * @return a copy of the item, or null when the bone shows its own cubes
     */
    @Nullable ItemStack item();

    /**
     * Show an item in place of the bone's own cubes, moved by the bone's pivot and animation.
     * Handy for a held sword or a swappable hat. Same as
     * {@code itemSource(ItemSource.fixed(item))}.
     *
     * @param item the item, copied; null shows the bone's own cubes again, an empty stack shows
     *     nothing
     * @return this object
     */
    S item(@Nullable ItemStack item);

    /**
     * Return what picks the item this bone shows.
     *
     * @return the source, or null when the bone shows its own cubes
     */
    @Nullable ItemSource itemSource();

    /**
     * Let a source pick the item this bone shows, so it can follow the entity's hands, the
     * viewer or anything else.
     *
     * @param source the source, or null to show the bone's own cubes again
     * @return this object
     */
    S itemSource(@Nullable ItemSource source);
}
