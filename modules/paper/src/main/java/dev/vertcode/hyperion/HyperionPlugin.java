package dev.vertcode.hyperion;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.Hyperion;
import dev.vertcode.hyperion.api.event.BlueprintLoadEvent;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.bedrock.BedrockLink;
import dev.vertcode.hyperion.bedrock.BedrockPlayers;
import dev.vertcode.hyperion.bedrock.BedrockToken;
import dev.vertcode.hyperion.behavior.BuiltinTypes;
import dev.vertcode.hyperion.behavior.HyperionBehaviorRegistry;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.HyperionFormatRegistry;
import dev.vertcode.hyperion.blueprint.ModelSpawner;
import dev.vertcode.hyperion.blueprint.ReloadResult;
import dev.vertcode.hyperion.command.HyperionCommands;
import dev.vertcode.hyperion.config.HyperionConfig;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.config.MessageBook;
import dev.vertcode.hyperion.effect.HyperionEffects;
import dev.vertcode.hyperion.listener.BaseEntityListener;
import dev.vertcode.hyperion.listener.BedrockListener;
import dev.vertcode.hyperion.listener.EffectListener;
import dev.vertcode.hyperion.listener.HitboxListener;
import dev.vertcode.hyperion.listener.ModelListener;
import dev.vertcode.hyperion.listener.PackListener;
import dev.vertcode.hyperion.listener.RiderInputListener;
import dev.vertcode.hyperion.listener.SeatListener;
import dev.vertcode.hyperion.model.HyperionModelRegistry;
import dev.vertcode.hyperion.model.ModelManager;
import dev.vertcode.hyperion.pack.PackFormat;
import dev.vertcode.hyperion.pack.PackManager;
import dev.vertcode.hyperion.ui.Menus;

public final class HyperionPlugin extends JavaPlugin {

    private static final String LOADED = "Loaded %d model(s) in %d ms: %d failed, %d warning(s)";
    private static final String LOAD_FAILED = "Loading the models failed";
    private static final String LINK_FAILED = "The Bedrock link could not start on port %d; is the port in use?";

    private @Nullable BlueprintRegistry registry;
    private @Nullable PackManager packs;
    private @Nullable ModelManager models;
    private @Nullable HyperionEffects effects;
    private List<PacketListenerCommon> packetListeners = List.of();
    private @Nullable BedrockLink bedrockLink;

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
        packs.bedrock(config.bedrock().enabled());
        this.packs = packs;

        // Bedrock
        BedrockPlayers bedrock = new BedrockPlayers();
        BedrockLink link = config.bedrock().enabled() ? this.startLink(config.bedrock().linkPort(), packs, bedrock) : null;
        this.bedrockLink = link;

        // Models
        HyperionBehaviorRegistry behaviors = new HyperionBehaviorRegistry();
        ModelManager models = new ModelManager(this, registry, behaviors, version, config.models(), config.pack());
        models.bedrock(bedrock, link);
        BuiltinTypes.register(this, behaviors, models);
        packs.hiddenBones(BuiltinTypes.hidesCubes(() -> models.settings().behaviors()));
        registry.useSpawner(models);
        packs.onBuilt(models::refresh);
        this.models = models;

        // Effects
        HyperionEffects effects = new HyperionEffects(this, models, registry, config.effects());
        models.effects(effects);
        async.execute(() -> effects.registerFolder(this, HyperionEffects.NAMESPACE, effects.prepareFolder()));
        this.effects = effects;

        // Api
        HyperionModelRegistry modelRegistry = new HyperionModelRegistry(registry, models, packs);
        Hyperion.install(new HyperionService(modelRegistry, models, behaviors, formats, effects));
        registry.reloadAll()
                .whenComplete(this::logLoad)
                .thenCompose(result -> packs.rebuild());

        // Listeners
        events.registerEvents(new PackListener(packs), this);
        events.registerEvents(new BedrockListener(bedrock), this);
        Menus menus = Menus.load(this, version);
        events.registerEvents(new ModelListener(models, modelRegistry, behaviors, effects, this, menus), this);
        events.registerEvents(new SeatListener(models), this);
        events.registerEvents(new EffectListener(effects.cameras()), this);
        EventManager packets = PacketEvents.getAPI().getEventManager();
        this.packetListeners = List.of(
                packets.registerListener(new BaseEntityListener(models), PacketListenerPriority.NORMAL),
                packets.registerListener(new HitboxListener(models), PacketListenerPriority.NORMAL),
                packets.registerListener(new RiderInputListener(models.interactions()), PacketListenerPriority.MONITOR)
        );

        // Commands
        HyperionCommands.register(this, registry, packs, models, effects);
    }

    @Override
    public void onDisable() {
        // Effects, then models
        if (this.effects != null) {
            this.effects.stopAll();
        }

        if (this.models != null) {
            this.models.shutdown();
        }

        for (PacketListenerCommon listener : this.packetListeners) {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        }

        if (this.bedrockLink != null) {
            this.bedrockLink.stop();
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

        if (this.effects != null) {
            this.effects.configure(config.effects());
            this.effects.reload();
        }
    }

    /**
     * Start the link the Geyser extension follows, with the token from
     * {@code bedrock-token.txt}, made on first start.
     *
     * @param port the port to listen on
     * @param packs where the Bedrock pack comes from
     * @param players where the extension's player reports go
     * @return the link, or null when it could not start, which is logged
     */
    private @Nullable BedrockLink startLink(final int port, final PackManager packs, final BedrockPlayers players) {
        try {
            String token = BedrockToken.load(this.getDataPath().resolve(BedrockToken.FILE));
            return BedrockLink.start(port, token, packs::bedrockPack, players);
        } catch (IOException exception) {
            this.getLogger().log(Level.SEVERE, String.format(LINK_FAILED, port), exception);
            return null;
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
