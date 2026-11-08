package dev.vertcode.hyperion.api.model.locator;

import org.bukkit.Location;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionfc;

import dev.vertcode.hyperion.api.model.Model;

/**
 * A locator on a live model, moving with its bone and animation.
 */
@ApiStatus.NonExtendable
public interface Locator {

    /**
     * Return the locator's blueprint.
     *
     * @return the blueprint
     */
    LocatorBlueprint blueprint();

    /**
     * Return the model this locator belongs to.
     *
     * @return the model
     */
    Model model();

    /**
     * Return the locator's name. Same as {@code blueprint().name()}.
     *
     * @return the name
     */
    String name();

    /**
     * Return where the locator is in the world, animation included, as of the last frame.
     *
     * @return a copy of the location, facing the locator's direction
     */
    Location location();

    /**
     * Return the locator's rotation in the world, as of the last frame.
     *
     * @return the world rotation
     */
    Quaternionfc rotation();
}
