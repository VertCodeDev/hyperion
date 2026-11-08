package dev.vertcode.hyperion.behavior;

import org.bukkit.plugin.Plugin;

import dev.vertcode.hyperion.api.model.behavior.BehaviorType;

/**
 * A behavior type and the plugin that registered it.
 *
 * @param owner the plugin, whose types go when it disables
 * @param type the type
 */
record RegisteredBehavior(Plugin owner, BehaviorType<?> type) {
}
