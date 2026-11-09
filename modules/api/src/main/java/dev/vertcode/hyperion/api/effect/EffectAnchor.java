package dev.vertcode.hyperion.api.effect;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import dev.vertcode.hyperion.api.model.Model;

/**
 * What an effect is attached to. The effect follows it every tick, until it is gone.
 *
 * <p>Offsets in an effect are blocks to the right, up and forward of the anchor. On a model, an
 * entity or a location they turn with its yaw; on a bone or a locator they turn with it fully, so
 * an effect on a jaw tilts with the jaw.
 */
public sealed interface EffectAnchor {

    /**
     * A fixed spot, facing the location's yaw.
     *
     * @param location the spot, copied
     * @return the anchor
     */
    static EffectAnchor at(final Location location) {
        return new AtLocation(location.clone());
    }

    /**
     * An entity's feet, facing where its body faces.
     *
     * @param entity the entity
     * @return the anchor
     */
    static EffectAnchor on(final Entity entity) {
        return new OnEntity(entity);
    }

    /**
     * A model's origin, facing where the model faces.
     *
     * @param model the model
     * @return the anchor
     */
    static EffectAnchor on(final Model model) {
        return new OnModel(model);
    }

    /**
     * A bone's pivot, turning with the bone.
     *
     * @param model the model
     * @param bone the bone's name
     * @return the anchor
     */
    static EffectAnchor bone(final Model model, final String bone) {
        return new OnBone(model, bone);
    }

    /**
     * A Blockbench locator, turning with its bone.
     *
     * @param model the model
     * @param locator the locator's name
     * @return the anchor
     */
    static EffectAnchor locator(final Model model, final String locator) {
        return new OnLocator(model, locator);
    }

    /**
     * A fixed spot.
     *
     * @param location the spot
     */
    record AtLocation(Location location) implements EffectAnchor {
    }

    /**
     * An entity.
     *
     * @param entity the entity
     */
    record OnEntity(Entity entity) implements EffectAnchor {
    }

    /**
     * A model's origin.
     *
     * @param model the model
     */
    record OnModel(Model model) implements EffectAnchor {
    }

    /**
     * A bone of a model.
     *
     * @param model the model
     * @param bone the bone's name
     */
    record OnBone(Model model, String bone) implements EffectAnchor {
    }

    /**
     * A locator of a model.
     *
     * @param model the model
     * @param locator the locator's name
     */
    record OnLocator(Model model, String locator) implements EffectAnchor {
    }
}
