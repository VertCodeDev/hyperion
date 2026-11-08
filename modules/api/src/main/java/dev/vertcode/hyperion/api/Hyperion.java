package dev.vertcode.hyperion.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.ModelRegistry;
import dev.vertcode.hyperion.api.model.ModelTracker;
import dev.vertcode.hyperion.api.model.behavior.BehaviorRegistry;
import dev.vertcode.hyperion.api.model.source.FormatRegistry;

/**
 * The way into Hyperion from another plugin.
 *
 * <p>Every method is a shortcut to {@link #api()}. The api is installed while Hyperion enables
 * and cleared when it disables, so call it from your own {@code onEnable} or later and declare
 * Hyperion as a dependency of your plugin. The same instance is registered in Bukkit's
 * {@code ServicesManager} under {@link HyperionApi}.
 *
 * <p>Every method is safe to call from any thread.
 */
public final class Hyperion {

    private static final String NOT_ENABLED = "Hyperion is not enabled: depend on it and call it from onEnable or later";

    private static volatile @Nullable HyperionApi installed;

    private Hyperion() {
    }

    /**
     * Return the installed api.
     *
     * @return the api
     * @throws IllegalStateException if Hyperion is not enabled
     */
    public static HyperionApi api() {
        HyperionApi current = installed;
        if (current == null) {
            throw new IllegalStateException(NOT_ENABLED);
        }

        return current;
    }

    /**
     * Return the registry of model blueprints.
     *
     * @return the model registry
     * @throws IllegalStateException if Hyperion is not enabled
     */
    public static ModelRegistry models() {
        return api().models();
    }

    /**
     * Return the tracker that finds live models.
     *
     * @return the model tracker
     * @throws IllegalStateException if Hyperion is not enabled
     */
    public static ModelTracker tracker() {
        return api().tracker();
    }

    /**
     * Return the registry of bone behaviors.
     *
     * @return the behavior registry
     * @throws IllegalStateException if Hyperion is not enabled
     */
    public static BehaviorRegistry behaviors() {
        return api().behaviors();
    }

    /**
     * Return the registry of model file formats.
     *
     * @return the format registry
     * @throws IllegalStateException if Hyperion is not enabled
     */
    public static FormatRegistry formats() {
        return api().formats();
    }

    /**
     * Install the api. Called by the Hyperion plugin while it enables.
     *
     * @param api the api to install
     */
    @ApiStatus.Internal
    public static void install(final HyperionApi api) {
        installed = api;
    }

    /**
     * Clear the api. Called by the Hyperion plugin while it disables.
     */
    @ApiStatus.Internal
    public static void uninstall() {
        installed = null;
    }
}
