package dev.vertcode.hyperion.api;

import org.jetbrains.annotations.ApiStatus;

import dev.vertcode.hyperion.api.effect.EffectRegistry;
import dev.vertcode.hyperion.api.model.ModelRegistry;
import dev.vertcode.hyperion.api.model.ModelTracker;
import dev.vertcode.hyperion.api.model.behavior.BehaviorRegistry;
import dev.vertcode.hyperion.api.model.source.FormatRegistry;

/**
 * Everything Hyperion offers another plugin, behind one interface.
 *
 * <p>Reach it through {@link Hyperion#api()} or Bukkit's {@code ServicesManager}. Every method is
 * safe from any thread. Being an interface, it can be mocked in your own tests.
 */
@ApiStatus.NonExtendable
public interface HyperionApi {

    /**
     * Return the registry of model blueprints.
     *
     * @return the model registry
     */
    ModelRegistry models();

    /**
     * Return the tracker that finds live models.
     *
     * @return the model tracker
     */
    ModelTracker tracker();

    /**
     * Return the registry of bone behaviors.
     *
     * @return the behavior registry
     */
    BehaviorRegistry behaviors();

    /**
     * Return the registry of model file formats.
     *
     * @return the format registry
     */
    FormatRegistry formats();

    /**
     * Return the effects and the way to play them.
     *
     * @return the effect registry
     */
    EffectRegistry effects();
}
