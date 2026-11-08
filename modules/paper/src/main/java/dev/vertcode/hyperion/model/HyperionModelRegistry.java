package dev.vertcode.hyperion.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.kyori.adventure.key.Key;
import org.bukkit.plugin.Plugin;

import dev.vertcode.hyperion.api.model.ModelBlueprint;
import dev.vertcode.hyperion.api.model.ModelRegistry;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.source.ModelSource;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.pack.PackManager;

/**
 * The api's model registry: the blueprint registry, with the resource pack rebuilt after every
 * change so new and reloaded models show, and live models removed with their blueprint.
 */
public final class HyperionModelRegistry implements ModelRegistry {

    private final BlueprintRegistry blueprints;
    private final ModelManager models;
    private final PackManager packs;

    /**
     * Create the registry.
     *
     * @param blueprints the blueprints
     * @param models the live models, removed when their blueprint goes
     * @param packs the resource pack, rebuilt after every change
     */
    public HyperionModelRegistry(final BlueprintRegistry blueprints, final ModelManager models, final PackManager packs) {
        this.blueprints = blueprints;
        this.models = models;
        this.packs = packs;
    }

    @Override
    public Optional<ModelBlueprint> find(final Key key) {
        return this.blueprints.find(key).map(blueprint -> blueprint);
    }

    @Override
    public Collection<ModelBlueprint> all() {
        List<ModelBlueprint> all = new ArrayList<>(this.blueprints.all());

        return List.copyOf(all);
    }

    @Override
    public CompletableFuture<ModelBlueprint> register(final Plugin owner, final Key key, final ModelSource source) {
        CompletableFuture<Blueprint> loaded = this.blueprints.register(owner, key, source);
        loaded.thenRun(this.packs::rebuild);

        return loaded.thenApply(blueprint -> blueprint);
    }

    @Override
    public boolean unregister(final Key key) {
        boolean removed = this.blueprints.unregister(key);
        if (removed) {
            this.models.removeAll(key, RemovalReason.BLUEPRINT_UNREGISTERED);
            this.packs.rebuild();
        }

        return removed;
    }

    @Override
    public void unregisterAll(final Plugin owner) {
        List<Key> keys = new ArrayList<>();
        for (Blueprint blueprint : this.blueprints.all()) {
            if (owner.equals(blueprint.owner())) {
                keys.add(blueprint.key());
            }
        }

        this.blueprints.unregisterAll(owner);
        for (Key key : keys) {
            this.models.removeAll(key, RemovalReason.PLUGIN_DISABLED);
        }

        if (!keys.isEmpty()) {
            this.packs.rebuild();
        }
    }

    @Override
    public CompletableFuture<ModelBlueprint> reload(final Key key) {
        CompletableFuture<Blueprint> loaded = this.blueprints.reload(key);
        loaded.thenRun(this.packs::rebuild);

        return loaded.thenApply(blueprint -> blueprint);
    }

    @Override
    public CompletableFuture<Void> reloadAll() {
        return this.blueprints.reloadAll()
                .thenCompose(result -> this.packs.rebuild())
                .thenAccept(pack -> {
                    // The pack listener moves live models onto the new blueprints.
                });
    }
}
