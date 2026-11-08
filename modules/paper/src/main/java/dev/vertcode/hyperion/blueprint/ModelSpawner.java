package dev.vertcode.hyperion.blueprint;

import java.util.function.Consumer;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.ModelAnchor;
import dev.vertcode.hyperion.api.model.SpawnOptions;

/**
 * Brings a blueprint to life, for {@link Blueprint#spawn} and {@link Blueprint#attach}.
 *
 * <p>Blueprints are plain data and know nothing of the world, so the registry hands each one a
 * spawner that forwards to the model manager once the plugin has enabled.
 */
@FunctionalInterface
public interface ModelSpawner {

    /**
     * The spawner before the plugin has enabled, and in tests: it refuses every spawn.
     */
    ModelSpawner UNAVAILABLE = (blueprint, anchor, options) -> {
        throw new IllegalStateException("Models can only be spawned while Hyperion is enabled");
    };

    /**
     * Spawn a model of a blueprint.
     *
     * @param blueprint the blueprint
     * @param anchor what drives the model's position
     * @param options the caller's settings, applied before anyone sees the model
     * @return the model
     */
    Model spawn(Blueprint blueprint, ModelAnchor anchor, Consumer<SpawnOptions> options);
}
