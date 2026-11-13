package dev.vertcode.hyperion.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.event.ModelSpawnEvent;
import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.EntityPart;
import dev.vertcode.hyperion.api.model.LocationAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.ModelAnchor;
import dev.vertcode.hyperion.api.model.ModelTracker;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.SpawnOptions;
import dev.vertcode.hyperion.api.version.Feature;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.bedrock.BedrockIds;
import dev.vertcode.hyperion.bedrock.BedrockLink;
import dev.vertcode.hyperion.bedrock.BedrockPlayers;
import dev.vertcode.hyperion.behavior.HyperionBehaviorRegistry;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.blueprint.ModelSpawner;
import dev.vertcode.hyperion.config.ModelSettings;
import dev.vertcode.hyperion.config.PackSettings;
import dev.vertcode.hyperion.debug.DebugViews;
import dev.vertcode.hyperion.pack.ResourcePack;
import dev.vertcode.hyperion.render.PartItems;
import dev.vertcode.hyperion.skin.HyperionSkins;

/**
 * Spawns models, keeps track of every live one, and follows reloads.
 *
 * <p>The indexes are concurrent, so the tracker methods, the packet listener and the entity
 * listeners read them from any thread. Spawning, refreshing and removing a model happen on the
 * thread that owns it; the methods here that start them from elsewhere hop there first.
 */
public final class ModelManager implements ModelSpawner, ModelTracker {

    private static final String UNREGISTERED = "The blueprint %s is no longer registered";
    private static final String INVALID_ENTITY = "The entity is not in a world";
    private static final String NO_WORLD = "The location has no world";
    private static final String NOT_OWNER = "Spawn a model on the thread that owns its %s";
    private static final String NOT_A_CARRIER = "%s cannot carry a model: it must be a mob that can be spawned";

    private final Plugin plugin;
    private final BlueprintRegistry registry;
    private final HyperionBehaviorRegistry behaviors;
    private final boolean itemModels;
    private final NamespacedKey carrierKey;
    private final Map<UUID, HyperionModel> models = new ConcurrentHashMap<>();
    private final Map<Integer, List<HyperionModel>> byEntity = new ConcurrentHashMap<>();
    private final ModelInteractions interactions = new ModelInteractions();
    private final DebugViews debug;
    private volatile ModelEffects effects = ModelEffects.NONE;
    private volatile ModelSettings settings;
    private volatile PartItems items;
    private volatile @Nullable HyperionSkins skins;
    private volatile String baseItem;
    private volatile @Nullable ResourcePack pack;
    private BedrockPlayers bedrockPlayers = new BedrockPlayers();
    private volatile @Nullable BedrockLink link;

    /**
     * Create the manager.
     *
     * @param plugin the plugin, for scheduling, events and logging
     * @param registry the blueprints, to follow reloads
     * @param behaviors the behavior types matched to bones
     * @param version the server's version, which decides how parts name their model
     * @param settings the model settings from {@code config.yml}
     * @param pack the pack settings from {@code config.yml}, for the base item
     * @param debug the debug views models draw
     */
    public ModelManager(
            final Plugin plugin,
            final BlueprintRegistry registry,
            final HyperionBehaviorRegistry behaviors,
            final MinecraftVersion version,
            final ModelSettings settings,
            final PackSettings pack,
            final DebugViews debug
    ) {
        this.plugin = plugin;
        this.debug = debug;
        this.carrierKey = new NamespacedKey(plugin, "carrier");
        this.registry = registry;
        this.behaviors = behaviors;
        this.itemModels = Feature.ITEM_MODEL_DEFINITIONS.availableOn(version);
        this.settings = settings;
        this.items = new PartItems(pack.baseItem(), this.itemModels);
        this.baseItem = pack.baseItem();
    }

    /**
     * Show models to Bedrock players through the Geyser extension. Call once, on enable.
     *
     * @param players who is on Bedrock
     * @param link the link the extension follows, or null with Bedrock off
     */
    public void bedrock(final BedrockPlayers players, final @Nullable BedrockLink link) {
        this.bedrockPlayers = players;
        this.link = link;
    }

    /**
     * Return whether a player is on Bedrock. Cheap, for every packet.
     *
     * @param player the player's id
     * @return true for a Bedrock player
     */
    public boolean isBedrock(final UUID player) {
        return this.bedrockPlayers.isBedrock(player);
    }

    /**
     * Return the link the Geyser extension follows.
     *
     * @return the link, or null with Bedrock off
     */
    @Nullable BedrockLink link() {
        return this.link;
    }

    /**
     * Use new settings from {@code config.yml}. The view distance and interpolation apply to
     * models spawned from now on; the base item reaches live models with the next pack.
     *
     * @param models the model settings
     * @param pack the pack settings
     */
    public void configure(final ModelSettings models, final PackSettings pack) {
        this.settings = models;
        this.items = new PartItems(pack.baseItem(), this.itemModels);
        boolean newItem = !pack.baseItem().equals(this.baseItem);
        this.baseItem = pack.baseItem();
        ResourcePack current = this.pack;
        if (newItem && current != null) {
            this.refreshAll(current, true);
        }
    }

    @Override
    public Model spawn(final Blueprint blueprint, final ModelAnchor anchor, final Consumer<SpawnOptions> options) {
        return this.spawn(blueprint, anchor, options, false);
    }

    /**
     * Spawn a model, as {@link #spawn(Blueprint, ModelAnchor, Consumer)} does, choosing whether
     * a player carrying it sees it too. A player never tracks themselves, so a model on a
     * player is normally shown to everyone but them.
     *
     * @param blueprint the blueprint
     * @param anchor what the model is on
     * @param options changes to the defaults
     * @param selfView true to show a model on a player to that player as well
     * @return the live model
     */
    public Model spawn(
            final Blueprint blueprint,
            final ModelAnchor anchor,
            final Consumer<SpawnOptions> options,
            final boolean selfView
    ) {
        Blueprint current = this.registry.find(blueprint.key())
                .orElseThrow(() -> new IllegalStateException(String.format(UNREGISTERED, blueprint.key().asString())));
        Set<EntityPart> hidden = EntityPart.NONE;
        Entity entity = null;
        switch (anchor) {
            case EntityAnchor(Entity carrier) -> {
                if (!carrier.isValid()) {
                    throw new IllegalStateException(INVALID_ENTITY);
                }

                if (!Bukkit.isOwnedByCurrentRegion(carrier)) {
                    throw new IllegalStateException(String.format(NOT_OWNER, "entity"));
                }

                hidden = EntityPart.ALL;
                entity = carrier;
            }
            case LocationAnchor(Location location) -> {
                if (location.getWorld() == null) {
                    throw new IllegalArgumentException(NO_WORLD);
                }

                if (!Bukkit.isOwnedByCurrentRegion(location)) {
                    throw new IllegalStateException(String.format(NOT_OWNER, "location"));
                }
            }
        }

        ModelSettings defaults = this.settings;
        ModelOptions chosen = new ModelOptions(current, defaults.viewDistance(), defaults.interpolation(), hidden);
        options.accept(chosen);
        chosen.selfView = selfView;
        HyperionModel model = new HyperionModel(this, current, anchor, chosen, this.pack);
        this.models.put(model.uniqueId(), model);
        if (entity != null) {
            this.byEntity.computeIfAbsent(entity.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(model);
        }

        model.start();
        Bukkit.getPluginManager().callEvent(new ModelSpawnEvent(model));

        return model;
    }

    /**
     * Spawn a mob to carry a model: hidden by the model, silent, and not saved with its chunk,
     * so it is gone after a restart. Removing the model removes the mob. Call on the thread that
     * owns the location.
     *
     * @param location where the mob spawns
     * @param type the mob's type, which must be a living entity that can be spawned
     * @return the mob
     * @throws IllegalArgumentException if the type cannot carry a model
     */
    public Entity spawnCarrier(final Location location, final EntityType type) {
        Class<? extends Entity> kind = type.getEntityClass();
        if (kind == null || !type.isSpawnable() || !type.isAlive() || location.getWorld() == null) {
            throw new IllegalArgumentException(String.format(NOT_A_CARRIER, type.getKey().asString()));
        }

        return location.getWorld().spawn(location, kind, carrier -> {
            carrier.setSilent(true);
            carrier.setPersistent(false);
            carrier.getPersistentDataContainer().set(this.carrierKey, PersistentDataType.BYTE, (byte) 1);
            if (carrier instanceof LivingEntity living) {
                living.setCanPickupItems(false);
            }
        });
    }

    /**
     * Return whether Hyperion spawned an entity to carry a model.
     *
     * @param entity the entity
     * @return true for a carrier, which goes when its model does
     */
    public boolean isCarrier(final Entity entity) {
        return entity.getPersistentDataContainer().has(this.carrierKey, PersistentDataType.BYTE);
    }

    /**
     * Follow a new pack: every live model moves to the blueprint now registered under its key
     * and to the pack's parts, or is removed when its key is gone. Safe from any thread.
     *
     * @param next the new pack
     */
    public void refresh(final ResourcePack next) {
        this.pack = next;
        this.refreshAll(next, false);
    }

    /**
     * Move every live model to the blueprint registered under its key and a pack's parts.
     *
     * @param next the pack
     * @param force true to send every model again even when nothing changed for it
     */
    private void refreshAll(final ResourcePack next, final boolean force) {
        for (HyperionModel model : this.models.values()) {
            Optional<Blueprint> latest = this.registry.find(model.currentBlueprint().key());
            if (latest.isEmpty()) {
                model.removeLater(RemovalReason.BLUEPRINT_UNREGISTERED);
                continue;
            }

            model.runOnOwner(() -> model.refresh(latest.get(), next, force));
        }
    }

    /**
     * Remove every model of a blueprint, each on the thread that owns it. Safe from any thread.
     *
     * @param key the blueprint's key
     * @param reason why they are removed
     */
    public void removeAll(final Key key, final RemovalReason reason) {
        for (HyperionModel model : this.models.values()) {
            if (model.currentBlueprint().key().equals(key)) {
                model.removeLater(reason);
            }
        }
    }

    /**
     * Remove a model on the thread that owns it. Safe from any thread.
     *
     * @param model the model
     * @param reason why it is removed
     */
    public void remove(final Model model, final RemovalReason reason) {
        if (model instanceof HyperionModel live) {
            live.removeLater(reason);
        }
    }

    /**
     * Remove every model, as Hyperion disables. On the thread that owns a model it is removed
     * with an event; elsewhere it is only taken down, since events cannot fire there.
     */
    public void shutdown() {
        for (HyperionModel model : List.copyOf(this.models.values())) {
            if (model.isOwnedHere()) {
                model.removeNow(RemovalReason.PLUGIN_DISABLED);
            } else {
                model.tearDown();
            }
        }
    }

    // Entity and player hooks

    /**
     * Show the models on an entity to a player who just started tracking it.
     *
     * @param player the player
     * @param entity the entity
     */
    public void tracked(final Player player, final Entity entity) {
        for (HyperionModel model : this.on(entity.getEntityId())) {
            model.tracked(player);
        }
    }

    /**
     * Hide the models on an entity from a player who stopped tracking it.
     *
     * @param player the player
     * @param entity the entity
     */
    public void untracked(final Player player, final Entity entity) {
        for (HyperionModel model : this.on(entity.getEntityId())) {
            model.untracked(player);
        }
    }

    /**
     * Remove the models on an entity that is leaving the world. Runs on the entity's thread.
     *
     * @param entity the entity
     * @param reason why the models go
     */
    public void entityRemoved(final Entity entity, final RemovalReason reason) {
        for (HyperionModel model : this.on(entity.getEntityId())) {
            model.removeNow(reason);
        }
    }

    /**
     * Forget a player whose client dropped every entity.
     *
     * @param player the player's id
     * @param left true when they left the server
     */
    public void lose(final UUID player, final boolean left) {
        for (HyperionModel model : this.models.values()) {
            model.lose(player, left);
        }
    }

    // Packet listener lookups

    /**
     * Return whether any entity carries a model, so the packet listener can skip every packet
     * when none does.
     *
     * @return true when at least one model is on an entity
     */
    public boolean anyOnEntities() {
        return !this.byEntity.isEmpty();
    }

    /**
     * Return the model a Bedrock player sees an entity as: the first model it carries.
     *
     * @param entityId the entity's id
     * @return the model's Bedrock id, or empty when the entity carries no model
     */
    public OptionalInt bedrockModel(final int entityId) {
        for (HyperionModel model : this.on(entityId)) {
            if (!model.isRemoved()) {
                return OptionalInt.of(BedrockIds.id(model.currentBlueprint().key()));
            }
        }

        return OptionalInt.empty();
    }

    /**
     * Return the parts the models a player sees on an entity hide from them.
     *
     * @param entityId the entity's id
     * @param viewer the player's id
     * @return the hidden parts, empty when they see no model on it
     */
    public Set<EntityPart> hiddenFor(final int entityId, final UUID viewer) {
        Set<EntityPart> hidden = null;
        for (HyperionModel model : this.on(entityId)) {
            if (model.isRemoved() || !model.seenBy(viewer)) {
                continue;
            }

            if (hidden == null) {
                hidden = EnumSet.noneOf(EntityPart.class);
            }

            hidden.addAll(model.hiddenParts());
        }

        return hidden == null ? EntityPart.NONE : hidden;
    }

    /**
     * Return the displays of every model a player sees on an entity, to mount on it.
     *
     * @param entityId the entity's id
     * @param viewer the player's id
     * @return the displays' entity ids
     */
    public int[] displaysFor(final int entityId, final UUID viewer) {
        List<HyperionModel> carried = this.on(entityId);
        if (carried.isEmpty()) {
            return new int[0];
        }

        List<int[]> parts = new ArrayList<>(carried.size());
        int total = 0;
        for (HyperionModel model : carried) {
            if (!model.isRemoved() && model.seenBy(viewer)) {
                int[] ids = model.displayIds();
                parts.add(ids);
                total += ids.length;
            }
        }

        int[] all = new int[total];
        int at = 0;
        for (int[] ids : parts) {
            System.arraycopy(ids, 0, all, at, ids.length);
            at += ids.length;
        }

        return all;
    }

    /**
     * Return everything riding an entity for one player: its real passengers and the displays of
     * the models they see on it. Call on the entity's thread.
     *
     * @param entity the entity
     * @param viewer the player's id
     * @return the entity ids
     */
    int[] passengers(final Entity entity, final UUID viewer) {
        List<Entity> riders = entity.getPassengers();
        int[] ids = new int[riders.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = riders.get(i).getEntityId();
        }

        return this.riders(entity.getEntityId(), ids, viewer);
    }

    /**
     * Turn an entity's real passengers into what one player sees riding it. A player who sees a
     * model on the entity gets the model's displays added, and the seats left out: those are
     * placed at their bones instead, with whoever sits in them. Safe from any thread.
     *
     * @param entityId the entity's id
     * @param riders the entity ids of its real passengers
     * @param viewer the player's id
     * @return the entity ids the player should see riding it
     */
    public int[] riders(final int entityId, final int[] riders, final UUID viewer) {
        boolean seen = false;
        for (HyperionModel model : this.on(entityId)) {
            if (!model.isRemoved() && model.seenBy(viewer)) {
                seen = true;
                break;
            }
        }

        if (!seen) {
            return riders;
        }

        int[] displays = this.displaysFor(entityId, viewer);
        int[] all = new int[riders.length + displays.length];
        int at = 0;
        for (int rider : riders) {
            if (!this.interactions.isSeat(rider)) {
                all[at++] = rider;
            }
        }

        System.arraycopy(displays, 0, all, at, displays.length);

        return Arrays.copyOf(all, at + displays.length);
    }

    /**
     * Return what the packet listeners look up: hitboxes, seats and rider keys.
     *
     * @return the lookups
     */
    public ModelInteractions interactions() {
        return this.interactions;
    }

    /**
     * Return the debug views models draw.
     *
     * @return the views
     */
    public DebugViews debug() {
        return this.debug;
    }

    /**
     * Play the effect keyframes models' animations pass from now on. Call once, on enable.
     *
     * @param effects what plays them
     */
    public void effects(final ModelEffects effects) {
        this.effects = effects;
    }

    /**
     * Return what plays the effect keyframes models' animations pass.
     *
     * @return the effects
     */
    ModelEffects effects() {
        return this.effects;
    }

    /**
     * Run a task inside a model's frames, from its next frame on, until it is done or the model
     * is removed. Safe from any thread.
     *
     * @param model the model
     * @param task the task
     * @return false when the model is not one of Hyperion's or is removed
     */
    public boolean attach(final Model model, final ModelTask task) {
        if (!(model instanceof HyperionModel live) || live.isRemoved()) {
            return false;
        }

        live.runOnOwner(() -> live.attach(task));

        return true;
    }

    /**
     * Run something on the thread that owns a model, from any thread.
     *
     * @param model the model
     * @param task what to run
     */
    public void runOnOwner(final Model model, final Runnable task) {
        if (model instanceof HyperionModel live) {
            live.runOnOwner(task);
        }
    }

    // Internals for models

    /**
     * Return the models on an entity.
     *
     * @param entityId the entity's id
     * @return the models, empty when none
     */
    private List<HyperionModel> on(final int entityId) {
        List<HyperionModel> carried = this.byEntity.get(entityId);

        return carried == null ? List.of() : carried;
    }

    /**
     * Drop a removed model from every index.
     *
     * @param model the model
     */
    void forget(final HyperionModel model) {
        this.models.remove(model.uniqueId());
        Entity entity = model.entity();
        if (entity == null) {
            return;
        }

        this.byEntity.computeIfPresent(entity.getEntityId(), (id, carried) -> {
            carried.remove(model);
            return carried.isEmpty() ? null : carried;
        });
    }

    /**
     * Return a player's PacketEvents connection.
     *
     * @param player the player
     * @return the connection, or null while PacketEvents does not know the player yet
     */
    @Nullable User user(final Player player) {
        return PacketEvents.getAPI().getPlayerManager().getUser(player);
    }

    Plugin plugin() {
        return this.plugin;
    }

    /**
     * Return the model settings in use.
     *
     * @return the settings from {@code config.yml}
     */
    public ModelSettings settings() {
        return this.settings;
    }

    PartItems items() {
        return this.items;
    }

    /**
     * Use the skin service, which gives a model on a player that player's skin. Call once, on
     * enable.
     *
     * @param skins the skin service
     */
    public void skins(final HyperionSkins skins) {
        this.skins = skins;
    }

    @Nullable HyperionSkins skins() {
        return this.skins;
    }

    HyperionBehaviorRegistry behaviors() {
        return this.behaviors;
    }

    // Tracker

    @Override
    public List<Model> of(final Entity entity) {
        return List.copyOf(this.on(entity.getEntityId()));
    }

    @Override
    public Optional<Model> byId(final UUID id) {
        return Optional.ofNullable(this.models.get(id));
    }

    @Override
    public List<Model> near(final Location center, final double radius) {
        double range = radius * radius;
        List<Model> near = new ArrayList<>();
        for (HyperionModel model : this.models.values()) {
            Location at = model.location();
            if (at.getWorld() != null && at.getWorld().equals(center.getWorld()) && at.distanceSquared(center) <= range) {
                near.add(model);
            }
        }

        return List.copyOf(near);
    }

    /**
     * Find the model nearest to a location.
     *
     * @param center where to measure from
     * @param radius how far to look
     * @return the nearest model, or empty when none is within the radius
     */
    public Optional<Model> nearest(final Location center, final double radius) {
        Model best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Model model : this.near(center, radius)) {
            double distance = model.location().distanceSquared(center);
            if (distance < bestDistance) {
                best = model;
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }

    /**
     * Return every live model.
     *
     * @return the models
     */
    public List<Model> all() {
        return List.copyOf(this.models.values());
    }
}
