package dev.vertcode.hyperion;

import java.util.concurrent.Executor;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.HyperionFormatRegistry;
import dev.vertcode.hyperion.blueprint.ReloadResult;
import dev.vertcode.hyperion.command.HyperionCommands;
import dev.vertcode.hyperion.config.HyperionConfig;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.config.MessageBook;
import dev.vertcode.hyperion.listener.PackListener;
import dev.vertcode.hyperion.pack.PackFormat;
import dev.vertcode.hyperion.pack.PackManager;

public final class HyperionPlugin extends JavaPlugin {

    private static final String LOADED = "Loaded %d model(s) in %d ms: %d failed, %d warning(s)";
    private static final String LOAD_FAILED = "Loading the models failed";

    private @Nullable PackManager packs;

    @Override
    public void onEnable() {
        // Files
        HyperionMessages.install(MessageBook.load(this));
        HyperionConfig config = HyperionConfig.load(this);

        // Blueprints
        Executor async = task -> this.getServer().getAsyncScheduler().runNow(this, scheduled -> task.run());
        BlueprintRegistry registry = new BlueprintRegistry(
                this.getDataPath().resolve("models"),
                new HyperionFormatRegistry(),
                async,
                this.getLogger()
        );

        // Resource pack
        MinecraftVersion version = MinecraftVersion.current();
        PackFormat format = PackFormat.ofServer(version, this.getServer().getClass().getClassLoader());
        PackManager packs = new PackManager(this, registry, async, config.pack(), version, format);
        packs.configure(config.pack());
        this.packs = packs;
        registry.reloadAll()
                .whenComplete(this::logLoad)
                .thenCompose(result -> packs.rebuild());

        // Listeners
        this.getServer().getPluginManager().registerEvents(new PackListener(packs), this);

        // Commands
        HyperionCommands.register(this, registry, packs);
    }

    @Override
    public void onDisable() {
        // Resource pack
        if (this.packs != null) {
            this.packs.shutdown();
        }
    }

    /**
     * Read {@code messages.yml} and {@code config.yml} again and use them from now on. Reads
     * files, so call it off the tick threads.
     */
    public void reloadFiles() {
        HyperionMessages.install(MessageBook.load(this));
        if (this.packs != null) {
            this.packs.configure(HyperionConfig.load(this).pack());
        }
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
