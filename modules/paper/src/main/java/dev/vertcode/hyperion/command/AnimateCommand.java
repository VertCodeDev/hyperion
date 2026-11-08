package dev.vertcode.hyperion.command;

import java.util.List;
import java.util.Optional;

import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.model.ModelManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion animate play <animation> [speed]} and {@code /hyperion animate stop
 * [animation]}: try a model's animations on the model nearest to you.
 *
 * <p>The animator is safe from any thread, so the command plays and stops right away, whichever
 * region owns the model.
 */
public final class AnimateCommand {

    private static final double RADIUS = 16d;
    private static final String SUGGESTIONS = "animations";
    private static final int FADE = 3;

    private final ModelManager models;

    /**
     * Create the commands.
     *
     * @param models the live models
     */
    public AnimateCommand(final ModelManager models) {
        this.models = models;
    }

    /**
     * Play an animation on the nearest model.
     *
     * @param source the player
     * @param animation the animation's name
     * @param speed how fast, 1 when left out
     */
    @Command(HyperionCommands.ROOT + " animate play <animation> [speed]")
    @CommandDescription("Play an animation on the model nearest to you")
    @Permission(HyperionPermissions.ANIMATE)
    public void play(
            final PlayerSource source,
            @Argument(value = "animation", suggestions = SUGGESTIONS) final String animation,
            @Argument("speed") @Range(min = "0.05", max = "10") final @Nullable Float speed
    ) {
        Player player = source.source();
        Model model = this.nearest(player);
        if (model == null) {
            return;
        }

        String key = model.blueprint().key().asString();
        if (!model.blueprint().animations().contains(animation)) {
            HyperionMessages.ANIMATE__NO_ANIMATION.send(player, value("model", key), value("animation", animation));
            return;
        }

        model.animator().play(animation, options -> options.speed(speed == null ? 1f : speed).fadeIn(FADE).fadeOut(FADE));
        HyperionMessages.ANIMATE__PLAYING.send(player, value("animation", animation), value("model", key));
    }

    /**
     * Stop one animation, or every animation, on the nearest model.
     *
     * @param source the player
     * @param animation the animation to stop, or null for every one
     */
    @Command(HyperionCommands.ROOT + " animate stop [animation]")
    @CommandDescription("Stop an animation, or all of them, on the model nearest to you")
    @Permission(HyperionPermissions.ANIMATE)
    public void stop(
            final PlayerSource source,
            @Argument(value = "animation", suggestions = SUGGESTIONS) final @Nullable String animation
    ) {
        Player player = source.source();
        Model model = this.nearest(player);
        if (model == null) {
            return;
        }

        String key = model.blueprint().key().asString();
        if (animation == null) {
            model.animator().stopAll();
            HyperionMessages.ANIMATE__STOPPED_ALL.send(player, value("model", key));
            return;
        }

        model.animator().stop(animation);
        HyperionMessages.ANIMATE__STOPPED.send(player, value("animation", animation), value("model", key));
    }

    /**
     * Suggest the animations of the model nearest to the player typing.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the animation names
     */
    @Suggestions(SUGGESTIONS)
    public List<String> suggest(final CommandContext<Source> context, final String input) {
        if (!(context.sender().source() instanceof Player player)) {
            return List.of();
        }

        Optional<Model> model = this.models.nearest(CommandText.where(player), RADIUS);

        return model.map(found -> List.copyOf(found.blueprint().animations())).orElse(List.of());
    }

    /**
     * Find the model nearest to the player, telling them when there is none.
     *
     * @param player the player
     * @return the model, or null when none is near
     */
    private @Nullable Model nearest(final Player player) {
        Optional<Model> model = this.models.nearest(CommandText.where(player), RADIUS);
        if (model.isEmpty()) {
            HyperionMessages.ANIMATE__NO_MODEL.send(player, value("radius", (int) RADIUS));
            return null;
        }

        return model.get();
    }
}
