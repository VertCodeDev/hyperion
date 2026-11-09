package dev.vertcode.hyperion.api.model.behavior.builtin;

import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;

/**
 * A bone that is never drawn, kept for its children, its locators and other behaviors.
 *
 * <p>Only the bone's own cubes are left out; bones below it still show.
 */
@ApiStatus.NonExtendable
public interface GhostBehavior extends BoneBehavior {
}
