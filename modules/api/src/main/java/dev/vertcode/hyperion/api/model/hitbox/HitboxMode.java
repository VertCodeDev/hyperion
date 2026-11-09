package dev.vertcode.hyperion.api.model.hitbox;

/**
 * How players hit and click a model: on its hitbox bones, on one box around all of it, or not at
 * all.
 *
 * <p>{@code config.yml} sets the default; a model can pick its own with
 * {@code SpawnOptions.hitboxMode}. With a hitbox, a hit on a model on an entity reaches that
 * entity, and every hit and click fires {@code ModelInteractEvent}.
 */
public enum HitboxMode {

    /**
     * A hitbox for every hitbox bone, which follows the bone as it moves. A model without
     * hitbox bones gets one box around all of it, as with {@link #MODEL}.
     */
    BONES,

    /**
     * One box around the whole model at rest, whatever bones it has.
     */
    MODEL,

    /**
     * No hitbox at all: players aim straight through the model, at whatever stands behind it.
     * For a model that is only scenery, or whose clicks a plugin catches with a box of its own.
     */
    NONE
}
