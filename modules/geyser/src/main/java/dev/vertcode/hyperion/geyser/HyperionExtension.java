package dev.vertcode.hyperion.geyser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.data.GeyserEntityDataTypes;
import org.geysermc.geyser.api.entity.property.type.GeyserBooleanEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionJoinEvent;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineResourcePacksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Show Hyperion models to Bedrock players as native Bedrock entities.
 *
 * <p>At startup the extension downloads every linked server's model list and Bedrock pack,
 * registers each model as a custom Bedrock entity with one property per animation, and hands the
 * packs to Geyser for Bedrock players to download. When a Java entity spawns for a Bedrock player
 * with a UUID that names a model, Geyser shows that model instead. The servers' event streams then
 * say which animations play, which the extension turns into property updates, so animations run
 * on the player's device.
 *
 * <p>Models are registered once, at Geyser's startup: a model added to a server later shows on
 * Bedrock after Geyser restarts.
 */
public final class HyperionExtension implements Extension {

    private static final long MARKER = 0x4879706572696F6EL;
    private static final float MAX_SPEED = 16f;
    private static final String NO_SERVERS = "No Hyperion server is linked: add one to %s";
    private static final String LINKED = "Linked to Hyperion at %s: %d model(s)";
    private static final String LINK_FAILED = "Could not link to Hyperion at %s: %s";

    private final List<LinkClient> clients = new ArrayList<>();
    private final Map<Integer, BedrockModel> models = new LinkedHashMap<>();
    private final Map<Integer, CustomEntityDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<Integer, List<GeyserBooleanEntityProperty>> animations = new ConcurrentHashMap<>();
    private final Map<Integer, GeyserFloatEntityProperty> speeds = new ConcurrentHashMap<>();
    private final Map<SeenEntity, ModelState> states = new ConcurrentHashMap<>();
    private final Map<SeenEntity, Integer> seen = new ConcurrentHashMap<>();
    private final List<Path> packs = new ArrayList<>();
    private boolean loaded;

    // Startup

    /**
     * Register every model as a custom Bedrock entity.
     *
     * @param event the event
     */
    @Subscribe
    public void onDefineEntities(final GeyserDefineEntitiesEvent event) {
        this.load();
        Set<String> registered = new HashSet<>();
        for (BedrockModel model : this.models.values()) {
            CustomEntityDefinition definition = CustomEntityDefinition.of(Identifier.of(model.identifier()));
            this.definitions.put(model.id(), definition);
            if (registered.add(model.identifier())) {
                event.register(definition);
            }
        }
    }

    /**
     * Register each model's properties: one switch per animation and the speed.
     *
     * @param event the event
     */
    @Subscribe
    public void onDefineProperties(final GeyserDefineEntityPropertiesEvent event) {
        this.load();
        for (BedrockModel model : this.models.values()) {
            Identifier entity = Identifier.of(model.identifier());
            List<GeyserBooleanEntityProperty> switches = new ArrayList<>();
            for (int i = 0; i < model.animations().size(); i++) {
                switches.add(event.registerBooleanProperty(entity, Identifier.of("hyperion:a" + i), false));
            }

            this.animations.put(model.id(), List.copyOf(switches));
            this.speeds.put(model.id(), event.registerFloatProperty(entity, Identifier.of("hyperion:speed"), 0f, MAX_SPEED, 1f));
        }
    }

    /**
     * Hand the downloaded Bedrock packs to Geyser.
     *
     * @param event the event
     */
    @Subscribe
    public void onDefinePacks(final GeyserDefineResourcePacksEvent event) {
        this.load();
        for (Path pack : this.packs) {
            event.register(ResourcePack.create(PackCodec.path(pack)));
        }
    }

    /**
     * Start following every server's event stream.
     *
     * @param event the event
     */
    @Subscribe
    public void onPostInitialize(final GeyserPostInitializeEvent event) {
        this.load();
        for (LinkClient client : this.clients) {
            client.follow(this::handle);
        }
    }

    /**
     * Stop following the streams.
     *
     * @param event the event
     */
    @Subscribe
    public void onShutdown(final GeyserShutdownEvent event) {
        for (LinkClient client : this.clients) {
            client.stop();
        }
    }

    // Players

    /**
     * Tell every server a Bedrock player joined, so it sends them models the Bedrock way.
     *
     * @param event the event
     */
    @Subscribe
    public void onJoin(final SessionJoinEvent event) {
        List<UUID> player = List.of(event.connection().javaUuid());
        for (LinkClient client : this.clients) {
            client.report(player, List.of());
        }
    }

    /**
     * Tell every server a Bedrock player left, and forget what they saw.
     *
     * @param event the event
     */
    @Subscribe
    public void onDisconnect(final SessionDisconnectEvent event) {
        UUID player = event.connection().javaUuid();
        this.states.keySet().removeIf(entity -> entity.player().equals(player));
        this.seen.keySet().removeIf(entity -> entity.player().equals(player));
        for (LinkClient client : this.clients) {
            client.report(List.of(), List.of(player));
        }
    }

    /**
     * Show an entity whose UUID names a model as that model.
     *
     * @param event the event
     */
    @Subscribe
    public void onSpawn(final ServerSpawnEntityEvent event) {
        UUID uuid = event.uuid();
        if (uuid == null || uuid.getMostSignificantBits() != MARKER) {
            return;
        }

        int model = (int) (uuid.getLeastSignificantBits() >>> 32);
        CustomEntityDefinition definition = this.definitions.get(model);
        if (definition == null) {
            return;
        }

        SeenEntity entity = new SeenEntity(event.connection().javaUuid(), event.entityId());
        this.seen.put(entity, model);
        event.definition(definition);
        event.preSpawnConsumer(spawned -> this.apply(spawned, model, this.states.getOrDefault(entity, ModelState.IDLE)));
    }

    // The stream

    /**
     * Act on one event from a server's stream.
     *
     * @param event the event
     */
    private void handle(final JsonObject event) {
        String type = event.has("t") ? event.get("t").getAsString() : "";
        int entity = event.has("entity") ? event.get("entity").getAsInt() : 0;
        List<UUID> players = players(event.get("players"));
        if ("gone".equals(type)) {
            for (UUID player : players) {
                SeenEntity seenEntity = new SeenEntity(player, entity);
                this.states.remove(seenEntity);
                this.seen.remove(seenEntity);
            }

            return;
        }

        if (!"state".equals(type)) {
            return;
        }

        Set<Integer> playing = new HashSet<>();
        if (event.get("playing") instanceof JsonArray numbers) {
            numbers.forEach(number -> playing.add(number.getAsInt()));
        }

        float speed = event.has("speed") ? event.get("speed").getAsFloat() : 1f;
        float scale = event.has("scale") ? event.get("scale").getAsFloat() : 1f;
        ModelState state = new ModelState(playing, speed, scale);
        for (UUID player : players) {
            SeenEntity seenEntity = new SeenEntity(player, entity);
            this.states.put(seenEntity, state);
            Integer model = this.seen.get(seenEntity);
            GeyserConnection connection = GeyserApi.api().connectionByUuid(player);
            GeyserEntity spawned = connection == null ? null : connection.entities().byJavaId(entity);
            if (model != null && spawned != null) {
                this.apply(spawned, model, state);
            }
        }
    }

    /**
     * Show a state on a spawned model: its animation switches, speed and scale.
     *
     * @param entity the Bedrock entity
     * @param model the model's id
     * @param state the state
     */
    private void apply(final GeyserEntity entity, final int model, final ModelState state) {
        List<GeyserBooleanEntityProperty> switches = this.animations.getOrDefault(model, List.of());
        GeyserFloatEntityProperty speed = this.speeds.get(model);
        entity.updatePropertiesBatched(updater -> {
            for (int i = 0; i < switches.size(); i++) {
                updater.update(switches.get(i), state.playing().contains(i));
            }

            if (speed != null) {
                updater.update(speed, Math.clamp(state.speed(), 0f, MAX_SPEED));
            }
        }, true);
        entity.override(GeyserEntityDataTypes.SCALE, state.scale());
    }

    // Loading

    /**
     * Read the config and download every server's models and pack, once.
     */
    private synchronized void load() {
        if (this.loaded) {
            return;
        }

        this.loaded = true;
        List<LinkServer> servers;
        try {
            servers = LinkConfig.load(this.dataFolder());
        } catch (IOException | RuntimeException exception) {
            this.logger().error(String.format(LINK_FAILED, this.dataFolder(), exception.getMessage()));
            return;
        }

        if (servers.isEmpty()) {
            this.logger().warning(String.format(NO_SERVERS, this.dataFolder().resolve("config.json")));
            return;
        }

        for (int i = 0; i < servers.size(); i++) {
            LinkClient client = new LinkClient(servers.get(i), this.logger());
            try {
                List<BedrockModel> found = client.manifest();
                Path pack = this.dataFolder().resolve("packs").resolve("hyperion-" + i + ".mcpack");
                client.pack(pack);
                for (BedrockModel model : found) {
                    this.models.putIfAbsent(model.id(), model);
                }

                this.packs.add(pack);
                this.clients.add(client);
                this.logger().info(String.format(LINKED, client.url(), found.size()));
            } catch (IOException | RuntimeException exception) {
                this.logger().error(String.format(LINK_FAILED, client.url(), exception.getMessage()));
                this.clients.add(client);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static List<UUID> players(final @Nullable JsonElement element) {
        List<UUID> players = new ArrayList<>();
        if (element instanceof JsonArray array) {
            for (JsonElement entry : array) {
                try {
                    players.add(UUID.fromString(entry.getAsString()));
                } catch (IllegalArgumentException exception) {
                    // A broken entry is skipped; the rest of the event still counts.
                }
            }
        }

        return players;
    }
}
