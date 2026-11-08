package dev.vertcode.hyperion;

import java.util.concurrent.Executor;
import java.util.logging.Level;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.Hyperion;
import dev.vertcode.hyperion.api.event.BlueprintLoadEvent;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.behavior.HyperionBehaviorRegistry;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.HyperionFormatRegistry;
import dev.vertcode.hyperion.blueprint.ModelSpawner;
import dev.vertcode.hyperion.blueprint.ReloadResult;
import dev.vertcode.hyperion.command.HyperionCommands;
import dev.vertcode.hyperion.config.HyperionConfig;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.config.MessageBook;
import dev.vertcode.hyperion.listener.BaseEntityListener;
import dev.vertcode.hyperion.listener.ModelListener;
import dev.vertcode.hyperion.listener.PackListener;
import dev.vertcode.hyperion.model.HyperionModelRegistry;
import dev.vertcode.hyperion.model.ModelManager;
import dev.vertcode.hyperion.pack.PackFormat;
import dev.vertcode.hyperion.pack.PackManager;

public final class HyperionPlugin extends JavaPlugin {

    private static final String LOADED = "Loaded %d model(s) in %d ms: %d failed, %d warning(s)";
    private static final String LOAD_FAILED = "Loading the models failed";

    private @Nullable BlueprintRegistry registry;
    private @Nullable PackManager packs;
    private @Nullable ModelManager models;
    private @Nullable PacketListenerCommon packetListener;

    @Override
    public void onEnable() {
        // Files
        HyperionMessages.install(MessageBook.load(this));
        HyperionConfig config = HyperionConfig.load(this);

        // Blueprints
        Executor async = task -> this.getServer().getAsyncScheduler().runNow(this, scheduled -> task.run());
        HyperionFormatRegistry formats = new HyperionFormatRegistry();
        BlueprintRegistry registry = new BlueprintRegistry(
                this.getDataPath().resolve("models"),
                formats,
                async,
                this.getLogger()
        );
        PluginManager events = this.getServer().getPluginManager();
        registry.onLoad(blueprint -> events.callEvent(new BlueprintLoadEvent(blueprint)));
        this.registry = registry;

        // Resource pack
        MinecraftVersion version = MinecraftVersion.current();
        PackFormat format = PackFormat.ofServer(version, this.getServer().getClass().getClassLoader());
        PackManager packs = new PackManager(this, registry, async, config.pack(), version, format);
        packs.configure(config.pack());
        this.packs = packs;

        // Models
        HyperionBehaviorRegistry behaviors = new HyperionBehaviorRegistry();
        ModelManager models = new ModelManager(this, registry, behaviors, version, config.models(), config.pack());
        registry.useSpawner(models);
        packs.onBuilt(models::refresh);
        this.models = models;

        // Api
        HyperionModelRegistry modelRegistry = new HyperionModelRegistry(registry, models, packs);
        Hyperion.install(new HyperionService(modelRegistry, models, behaviors, formats));
        registry.reloadAll()
                .whenComplete(this::logLoad)
                .thenCompose(result -> packs.rebuild());

        // Listeners
        events.registerEvents(new PackListener(packs), this);
        events.registerEvents(new ModelListener(models, modelRegistry, behaviors), this);
        this.packetListener = PacketEvents.getAPI()
                .getEventManager()
                .registerListener(new BaseEntityListener(models), PacketListenerPriority.NORMAL);

        // Commands
        HyperionCommands.register(this, registry, packs, models);
    }

    @Override
    public void onDisable() {
        // Models
        if (this.models != null) {
            this.models.shutdown();
        }

        if (this.packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.packetListener);
        }

        // Api
        if (this.registry != null) {
            this.registry.useSpawner(ModelSpawner.UNAVAILABLE);
        }

        Hyperion.uninstall();

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
        HyperionConfig config = HyperionConfig.load(this);
        if (this.packs != null) {
            this.packs.configure(config.pack());
        }

        if (this.models != null) {
            this.models.configure(config.models(), config.pack());
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
