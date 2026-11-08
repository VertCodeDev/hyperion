package dev.vertcode.hyperion.api.model.behavior.builtin;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

/**
 * A line of text that follows a bone, such as a boss's name over its head.
 *
 * <p>Setters are safe from any thread and apply on the next frame.
 */
@ApiStatus.NonExtendable
public interface NametagBehavior extends BoneBehavior {

    /**
     * Return the text shown.
     *
     * @return the text, or null to show the carrying entity's custom name
     */
    @Nullable Component text();

    /**
     * Set the text shown.
     *
     * @param text the text, or null to show the carrying entity's custom name
     * @return this behavior
     */
    NametagBehavior text(@Nullable Component text);

    /**
     * Return whether the text shows through blocks.
     *
     * @return true when seen through blocks
     */
    boolean seeThrough();

    /**
     * Show the text through blocks or not.
     *
     * @param seeThrough true to show through blocks
     * @return this behavior
     */
    NametagBehavior seeThrough(boolean seeThrough);
}
