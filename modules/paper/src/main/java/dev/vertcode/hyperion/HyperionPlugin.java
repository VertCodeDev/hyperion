package dev.vertcode.hyperion;

import java.util.concurrent.Executor;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.HyperionFormatRegistry;
import dev.vertcode.hyperion.blueprint.ReloadResult;
import dev.vertcode.hyperion.command.HyperionCommands;

public final class HyperionPlugin extends JavaPlugin {

    private static final String LOADED = "Loaded %d model(s) in %d ms: %d failed, %d warning(s)";
    private static final String LOAD_FAILED = "Loading the models failed";

    @Override
    public void onEnable() {
        // Blueprints
        Executor async = task -> this.getServer().getAsyncScheduler().runNow(this, _ -> task.run());
        BlueprintRegistry registry = new BlueprintRegistry(
                this.getDataPath().resolve("models"),
                new HyperionFormatRegistry(),
                async,
                this.getLogger()
        );
        registry.reloadAll().whenComplete((result, error) -> this.logLoad(result, error));

        // Commands
        HyperionCommands.register(this, registry);
    }

    /**
     * Log how the first load of the models went.
     *
     * @param result what the load did, or null when it failed outright
     * @param error why it failed, or null when it worked
     */
    private void logLoad(final @Nullable ReloadResult result, final @Nullable Throwable error) {
        if (result == null) {
            this.getLogger().log(Level.SEVERE, LOAD_FAILED, error);
            return;
        }

        this.getLogger().info(String.format(
                LOADED,
                result.loaded().size(),
                result.millis(),
                result.failed().size(),
                result.warnings()
        ));
    }
}
