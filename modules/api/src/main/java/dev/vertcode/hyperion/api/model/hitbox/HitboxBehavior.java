package dev.vertcode.hyperion.api.model.hitbox;

import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

/**
 * A bone players can hit and click, sized from the bone's cubes unless you set it.
 *
 * <p>Hits and clicks fire {@code ModelInteractEvent}. Setters are safe from any thread and apply
 * on the next frame.
 */
@ApiStatus.NonExtendable
public interface HitboxBehavior extends BoneBehavior {

    /**
     * Return the hitbox's size and place, before the model's scale.
     *
     * @return the hitbox
     */
    Hitbox hitbox();

    /**
     * Resize or move the hitbox.
     *
     * @param hitbox the new hitbox, before the model's scale
     * @return this behavior
     */
    HitboxBehavior hitbox(Hitbox hitbox);

    /**
     * Return whether players can hit and click the hitbox.
     *
     * @return true when it reacts to players
     */
    boolean interactable();

    /**
     * Let players hit and click the hitbox, or not.
     *
     * @param interactable true to react to players
     * @return this behavior
     */
    HitboxBehavior interactable(boolean interactable);

    /**
     * Return whether hits on a model on an entity damage that entity.
     *
     * @return true when hits pass to the entity
     */
    boolean forwardsDamage();

    /**
     * Pass hits on to the entity carrying the model, as if the player hit the entity itself.
     *
     * @param forwards true to pass hits on; true by default
     * @return this behavior
     */
    HitboxBehavior forwardsDamage(boolean forwards);
}
