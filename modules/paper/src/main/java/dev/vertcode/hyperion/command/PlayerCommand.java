package dev.vertcode.hyperion.command;

import java.util.List;
import java.util.Optional;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.bukkit.annotation.specifier.DefaultNamespace;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.HyperionPlugin;
import dev.vertcode.hyperion.api.animation.LoopMode;
import dev.vertcode.hyperion.api.player.PlayerAnimation;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.blueprint.BlueprintRegistry;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.player.HyperionPlayerAnimations;
import dev.vertcode.hyperion.skin.HyperionSkins;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion player play <model> <animation> [player]} and {@code /hyperion player stop
 * [player]}: play an animation of a player model in a player's place, wearing their skin.
 *
 * <p>An animation on a player starts and ends on the thread that owns them, so the commands hand
 * the work to that player's scheduler.
 */
public final class PlayerCommand {

    private static final String SUGGESTIONS = "player-animations";
    private static final String MODEL = "model";

    private final HyperionPlugin plugin;
    private final BlueprintRegistry registry;
    private final HyperionPlayerAnimations animations;
    private final HyperionSkins skins;

    /**
     * Create the commands.
     *
     * @param plugin Hyperion, to run on a player's thread
     * @param registry the loaded blueprints
     * @param animations the animations on players, to play and stop
     * @param skins the skin service, to warn when the server cannot draw skins
     */
    public PlayerCommand(
            final HyperionPlugin plugin,
            final BlueprintRegistry registry,
            final HyperionPlayerAnimations animations,
            final HyperionSkins skins
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.animations = animations;
        this.skins = skins;
    }

    /**
     * Play an animation on yourself, or on another player.
     *
     * @param source who runs the command
     * @param key the player model's key; {@code hyperion:} may be left out
     * @param animation the animation to play
     * @param target the player to play it on, the sender when left out
     * @param lock true to hold the player in place while it plays
     * @param loop true to keep it playing until it is stopped
     * @param noWalk true to leave the model's idle and walk animations off
     * @param stopOnSneak true to end it when the player sneaks
     * @param stopOnDamage true to end it when the player is hurt
     */
    @Command(HyperionCommands.ROOT + " player play <model> <animation> [player]")
    @CommandDescription("Play an animation of a player model in your place, or another player's")
    @Permission(HyperionPermissions.PLAYER)
    public void play(
            final Source source,
            @Argument(value = MODEL, suggestions = BlueprintSuggestions.NAME)
            @DefaultNamespace(BlueprintKeys.NAMESPACE) final NamespacedKey key,
            @Argument(value = "animation", suggestions = SUGGESTIONS) final String animation,
            @Argument("player") final @Nullable SinglePlayerSelector target,
            @Flag("lock") final boolean lock,
            @Flag("loop") final boolean loop,
            @Flag("no-walk") final boolean noWalk,
            @Flag("stop-on-sneak") final boolean stopOnSneak,
            @Flag("stop-on-damage") final boolean stopOnDamage
    ) {
        CommandSender sender = source.source();
        Player player = this.target(sender, target);
        if (player == null) {
            return;
        }

        Blueprint blueprint = this.registry.find(Key.key(key.namespace(), key.value())).orElse(null);
        if (blueprint == null) {
            HyperionMessages.MODEL_NOT_LOADED.send(sender, value(MODEL, key.asString()));
            return;
        }

        String model = blueprint.key().asString();
        if (!blueprint.animations().contains(animation)) {
            HyperionMessages.PLAYER__NO_ANIMATION.send(sender, value(MODEL, model), value("animation", animation));
            return;
        }

        if (blueprint.skinned() && !this.skins.supported()) {
            HyperionMessages.PLAYER__NO_SKINS.send(sender);
        }

        player.getScheduler().run(this.plugin, task -> {
            Optional<PlayerAnimation> playing;
            try {
                playing = this.animations.play(player, blueprint, animation, options -> {
                    options.locked(lock).walkAnimations(!noWalk);
                    if (stopOnSneak) {
                        options.stopOnSneak(true);
                    }

                    if (stopOnDamage) {
                        options.stopOnDamage(true);
                    }

                    if (loop) {
                        options.loop(LoopMode.LOOP);
                    }
                });
            } catch (IllegalStateException | IllegalArgumentException exception) {
                HyperionMessages.PLAYER__FAILED.send(sender, value("reason", String.valueOf(exception.getMessage())));
                return;
            }

            if (playing.isEmpty()) {
                HyperionMessages.PLAYER__CANCELLED.send(sender);
                return;
            }

            HyperionMessages.PLAYER__PLAYING.send(sender, value("animation", animation), value("player", player.getName()));
        }, null);
    }

    /**
     * Stop the animation on yourself, or on another player.
     *
     * @param source who runs the command
     * @param target the player to stop, the sender when left out
     */
    @Command(HyperionCommands.ROOT + " player stop [player]")
    @CommandDescription("Stop the animation playing on you, or on another player")
    @Permission(HyperionPermissions.PLAYER)
    public void stop(final Source source, @Argument("player") final @Nullable SinglePlayerSelector target) {
        CommandSender sender = source.source();
        Player player = this.target(sender, target);
        if (player == null) {
            return;
        }

        if (this.animations.stop(player)) {
            HyperionMessages.PLAYER__STOPPED.send(sender, value("player", player.getName()));
            return;
        }

        HyperionMessages.PLAYER__NONE.send(sender, value("player", player.getName()));
    }

    /**
     * Suggest the animations of the model already typed.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the animation names
     */
    @Suggestions(SUGGESTIONS)
    public List<String> suggest(final CommandContext<Source> context, final String input) {
        Optional<NamespacedKey> key = context.optional(MODEL);

        return key.flatMap(typed -> this.registry.find(Key.key(typed.namespace(), typed.value())))
                .map(blueprint -> List.copyOf(blueprint.animations()))
                .orElse(List.of());
    }

    /**
     * Work out who a player command is about, telling the sender when it cannot be them.
     *
     * @param sender who runs the command
     * @param target the player they named, or null for themselves
     * @return the player, or null when there is none or the sender may not pick another
     */
    private @Nullable Player target(final CommandSender sender, final @Nullable SinglePlayerSelector target) {
        if (target == null) {
            if (sender instanceof Player self) {
                return self;
            }

            HyperionMessages.COMMAND__PLAYERS_ONLY.send(sender);
            return null;
        }

        Player player = target.single();
        if (!player.equals(sender) && !sender.hasPermission(HyperionPermissions.PLAYER_OTHERS)) {
            HyperionMessages.COMMAND__NO_PERMISSION.send(sender);
            return null;
        }

        return player;
    }
}
