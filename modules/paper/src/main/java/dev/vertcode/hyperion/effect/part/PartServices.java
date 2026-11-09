package dev.vertcode.hyperion.effect.part;

import org.bukkit.plugin.Plugin;

import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.model.ModelManager;

/**
 * What the built-in part types need from the plugin.
 *
 * @param plugin Hyperion, for scheduling
 * @param blueprints the loaded blueprints, for props that show one
 * @param models the live models, for removing those props from any thread
 * @param cameras the players a camera holds
 */
public record PartServices(Plugin plugin, BlueprintRegistry blueprints, ModelManager models, CameraLock cameras) {
}
