package dev.vertcode.hyperion.command;

import java.util.Locale;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.bukkit.annotation.specifier.DefaultNamespace;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.HyperionPlugin;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.model.ModelManager;
import dev.vertcode.hyperion.pack.PackManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion spawn <model> [scale] [--entity <mob>] [--static]} and
 * {@code /hyperion attach <model> [scale]}: put a model on a new mob where you stand, on its own,
 * or on the entity you look at.
 *
 * <p>A player's command runs on the player's region, which owns where they stand and the mob
 * spawned there. The entity they look at may belong to another region, so attaching hops to the
 * entity's scheduler.
 */
public final class SpawnCommand {

    private static final int TARGET_DISTANCE = 16;

    private final HyperionPlugin plugin;
    private final BlueprintRegistry registry;
    private final PackManager packs;
    private final ModelManager models;

    /**
     * Create the commands.
     *
     * @param plugin the plugin, for scheduling
     * @param registry where the blueprints come from
     * @param packs the resource pack, to warn when it is not built yet
     * @param models the live models, which spawn the mobs models ride
     */
    public SpawnCommand(
            final HyperionPlugin plugin,
            final BlueprintRegistry registry,
            final PackManager packs,
            final ModelManager models
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.packs = packs;
        this.models = models;
    }

    /**
     * Spawn a model where the player stands: on a new mob that walks around, or on its own.
     *
     * @param source the player
     * @param key the model's key; {@code hyperion:} may be left out
     * @param scale the model's scale, 1 when left out
     * @param mob the mob to spawn it on, the one in {@code config.yml} when left out
     * @param still true to place the model on its own, standing still
     */
    @Command(HyperionCommands.ROOT + " spawn <model> [scale]")
    @CommandDescription("Spawn a model on a mob where you stand, or on its own with --static")
    @Permission(HyperionPermissions.SPAWN)
    public void spawn(
            final PlayerSource source,
            @Argument(value = "model", suggestions = BlueprintSuggestions.NAME)
            @DefaultNamespace(BlueprintKeys.NAMESPACE) final NamespacedKey key,
            @Argument(value = "scale", suggestions = ValueSuggestions.SCALES) @Range(min = "0.05", max = "16")
            final @Nullable Float scale,
            @Flag(value = "entity", suggestions = ValueSuggestions.MOBS) final @Nullable String mob,
            @Flag("static") final boolean still
    ) {
        Player player = source.source();
        Blueprint blueprint = this.find(player, key);
        if (blueprint == null) {
            return;
        }

        float size = scale == null ? 1f : scale;
        String model = blueprint.key().asString();
        if (still) {
            try {
                blueprint.spawn(CommandText.where(player), options -> options.scale(size));
            } catch (IllegalStateException | IllegalArgumentException exception) {
                HyperionMessages.SPAWN__FAILED.send(player, value("reason", String.valueOf(exception.getMessage())));
                return;
            }

            HyperionMessages.SPAWN__SPAWNED.send(player, value("model", model));
            this.warnWithoutPack(player);
            return;
        }

        String name = mob == null ? this.models.settings().spawnEntity() : mob.toLowerCase(Locale.ROOT);
        NamespacedKey typeKey = NamespacedKey.fromString(name);
        EntityType type = typeKey == null ? null : Registry.ENTITY_TYPE.get(typeKey);
        if (type == null) {
            HyperionMessages.SPAWN__BAD_ENTITY.send(player, value("entity", name));
            return;
        }

        Entity carrier;
        try {
            carrier = this.models.spawnCarrier(CommandText.where(player), type);
        } catch (IllegalArgumentException exception) {
            HyperionMessages.SPAWN__BAD_ENTITY.send(player, value("entity", name));
            return;
        }

        try {
            blueprint.attach(carrier, options -> options.scale(size));
        } catch (IllegalStateException | IllegalArgumentException exception) {
            carrier.remove();
            HyperionMessages.SPAWN__FAILED.send(player, value("reason", String.valueOf(exception.getMessage())));
            return;
        }

        HyperionMessages.SPAWN__SPAWNED_ON_MOB.send(player, value("model", model), value("entity", type.getKey().getKey()));
        this.warnWithoutPack(player);
    }

    /**
     * Put a model on the entity the player looks at.
     *
     * @param source the player
     * @param key the model's key; {@code hyperion:} may be left out
     * @param scale the model's scale, 1 when left out
     */
    @Command(HyperionCommands.ROOT + " attach <model> [scale]")
    @CommandDescription("Put a model on the entity you look at")
    @Permission(HyperionPermissions.SPAWN)
    public void attach(
            final PlayerSource source,
            @Argument(value = "model", suggestions = BlueprintSuggestions.NAME)
            @DefaultNamespace(BlueprintKeys.NAMESPACE) final NamespacedKey key,
            @Argument(value = "scale", suggestions = ValueSuggestions.SCALES) @Range(min = "0.05", max = "16")
            final @Nullable Float scale
    ) {
        Player player = source.source();
        Blueprint blueprint = this.find(player, key);
        if (blueprint == null) {
            return;
        }

        Entity target = player.getTargetEntity(TARGET_DISTANCE);
        if (target == null) {
            HyperionMessages.SPAWN__NO_TARGET.send(player, value("distance", TARGET_DISTANCE));
            return;
        }

        target.getScheduler().run(
                this.plugin,
                task -> CommandText.reply(this.plugin, player, attachNow(blueprint, target, scale)),
                null
        );
        this.warnWithoutPack(player);
    }

    /**
     * Put a model on an entity, on the entity's thread.
     *
     * @param blueprint the blueprint
     * @param target the entity
     * @param scale the model's scale, or null for 1
     * @return the reply for the player
     */
    private static Component attachNow(final Blueprint blueprint, final Entity target, final @Nullable Float scale) {
        try {
            blueprint.attach(target, options -> options.scale(scale == null ? 1f : scale));
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return HyperionMessages.SPAWN__FAILED.component(value("reason", String.valueOf(exception.getMessage())));
        }

        return HyperionMessages.SPAWN__ATTACHED.component(
                value("model", blueprint.key().asString()),
                value("entity", target.getName())
        );
    }

    /**
     * Find a loaded blueprint, telling the player when there is none.
     *
     * @param player the player
     * @param key the key they typed
     * @return the blueprint, or null when none is loaded under the key
     */
    private @Nullable Blueprint find(final Player player, final NamespacedKey key) {
        Blueprint blueprint = this.registry.find(Key.key(key.namespace(), key.value())).orElse(null);
        if (blueprint == null) {
            HyperionMessages.MODEL_NOT_LOADED.send(player, value("model", key.asString()));
        }

        return blueprint;
    }

    /**
     * Tell the player the model stays invisible until the pack is built.
     *
     * @param player the player
     */
    private void warnWithoutPack(final Player player) {
        if (this.packs.current().isEmpty()) {
            HyperionMessages.SPAWN__PACK_NOT_BUILT.send(player);
        }
    }
}
