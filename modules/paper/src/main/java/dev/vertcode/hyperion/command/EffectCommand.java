package dev.vertcode.hyperion.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Range;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectAnchor;
import dev.vertcode.hyperion.api.effect.EffectHandle;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.effect.HyperionEffects;
import dev.vertcode.hyperion.model.ModelManager;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion effect play|stop|list}: try effects on the model nearest to you, or where
 * you stand.
 */
public final class EffectCommand {

    private static final double RADIUS = 16d;
    private static final int PAGE_SIZE = 10;
    private static final String SUGGESTIONS = "effects";

    private final HyperionEffects effects;
    private final ModelManager models;

    /**
     * Create the commands.
     *
     * @param effects the effects
     * @param models the live models, to find the nearest
     */
    public EffectCommand(final HyperionEffects effects, final ModelManager models) {
        this.effects = effects;
        this.models = models;
    }

    /**
     * Play an effect on the nearest model, or where the player stands.
     *
     * @param source the player
     * @param name the effect's key, {@code hyperion:} when left out
     * @param here true to play it where the player stands, even near a model
     * @param camera true to let the effect's camera take the player's view
     */
    @Command(HyperionCommands.ROOT + " effect play <effect>")
    @CommandDescription("Play an effect on the model nearest to you, or where you stand")
    @Permission(HyperionPermissions.EFFECT)
    public void play(
            final PlayerSource source,
            @Argument(value = "effect", suggestions = SUGGESTIONS) final String name,
            @Flag("here") final boolean here,
            @Flag("camera") final boolean camera
    ) {
        Player player = source.source();
        Key key = key(name);
        Effect effect = key == null ? null : this.effects.find(key).orElse(null);
        if (effect == null) {
            HyperionMessages.EFFECT__NO_EFFECT.send(player, value("effect", name));
            return;
        }

        Location at = CommandText.where(player);
        Optional<Model> model = here ? Optional.empty() : this.models.nearest(at, RADIUS);
        EffectAnchor anchor = model.map(EffectAnchor::on).orElseGet(() -> EffectAnchor.at(at));
        this.effects.play(effect, anchor, options -> {
            if (camera) {
                options.camera(player);
            }
        });

        Component target = model
                .map(found -> HyperionMessages.EFFECT__ON_MODEL.component(value("model", found.blueprint().key().asString())))
                .orElseGet(HyperionMessages.EFFECT__HERE::component);
        HyperionMessages.EFFECT__PLAYING.send(player, value("effect", effect.key().asString()), value("target", target));
    }

    /**
     * Stop every effect near the player.
     *
     * @param source the player
     */
    @Command(HyperionCommands.ROOT + " effect stop")
    @CommandDescription("Stop every effect near you")
    @Permission(HyperionPermissions.EFFECT)
    public void stop(final PlayerSource source) {
        Player player = source.source();
        Location at = CommandText.where(player);
        int stopped = 0;
        for (EffectHandle handle : this.effects.playing()) {
            Location there = handle.location();
            if (there.getWorld() != null && there.getWorld().equals(at.getWorld()) && there.distanceSquared(at) <= RADIUS * RADIUS) {
                handle.stop();
                stopped++;
            }
        }

        HyperionMessages.EFFECT__STOPPED.send(player, value("count", stopped), value("radius", (int) RADIUS));
    }

    /**
     * List one page of the loaded effects.
     *
     * @param source who ran the command
     * @param page the page to show, 1 when left out
     */
    @Command(HyperionCommands.ROOT + " effect list")
    @CommandDescription("List every loaded effect")
    @Permission(HyperionPermissions.EFFECT)
    public void list(final Source source, @Flag("page") @Range(min = "1") final @Nullable Integer page) {
        CommandSender sender = source.source();
        List<Effect> all = new ArrayList<>(this.effects.all());
        if (all.isEmpty()) {
            HyperionMessages.EFFECT__EMPTY.send(sender);
            return;
        }

        all.sort(Comparator.comparing(effect -> effect.key().asString()));
        int pages = (all.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int shown = page == null ? 1 : page;
        if (shown > pages) {
            HyperionMessages.LIST__NO_PAGE.send(sender, value("page", shown), value("pages", pages));
            return;
        }

        TextComponent.Builder message = Component.text()
                .append(HyperionMessages.EFFECT__HEADING.component(value("count", all.size())));
        int first = (shown - 1) * PAGE_SIZE;
        for (Effect effect : all.subList(first, Math.min(first + PAGE_SIZE, all.size()))) {
            message.appendNewline().append(entry(effect));
        }

        if (pages > 1) {
            message.appendNewline().append(footer(shown, pages));
        }

        sender.sendMessage(message.build());
    }

    /**
     * Suggest every loaded effect's key.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the keys, sorted
     */
    @Suggestions(SUGGESTIONS)
    public List<String> suggest(final CommandContext<Source> context, final String input) {
        List<String> keys = new ArrayList<>();
        for (Effect effect : this.effects.all()) {
            keys.add(effect.key().asString());
        }

        keys.sort(String::compareTo);

        return keys;
    }

    /**
     * Read an effect's key as typed, in Hyperion's namespace when it has none.
     *
     * @param name what was typed
     * @return the key, or null when it cannot be one
     */
    private static @Nullable Key key(final String name) {
        String lowered = name.trim().toLowerCase(Locale.ROOT);
        try {
            return lowered.contains(":") ? Key.key(lowered) : Key.key(HyperionEffects.NAMESPACE, lowered);
        } catch (InvalidKeyException exception) {
            return null;
        }
    }

    /**
     * Write one effect's line, a click away from playing it.
     *
     * @param effect the effect
     * @return the line
     */
    private static Component entry(final Effect effect) {
        String key = effect.key().asString();
        Component line = HyperionMessages.EFFECT__ENTRY.component(
                value("effect", key),
                value("parts", effect.parts().size()),
                value("duration", effect.duration()),
                value("loop", effect.loop() ? HyperionMessages.EFFECT__ENTRY_LOOP.component() : Component.empty())
        );

        return CommandText.clickable(line, "/hyperion effect play " + key, HyperionMessages.EFFECT__ENTRY_HOVER.component());
    }

    /**
     * Write the line under a page: the page number between buttons to the pages around it.
     *
     * @param page the page shown
     * @param pages how many pages there are
     * @return the line
     */
    private static Component footer(final int page, final int pages) {
        Component previous = page > 1
                ? button(HyperionMessages.LIST__PREVIOUS, page - 1)
                : HyperionMessages.LIST__PREVIOUS_NONE.component();
        Component next = page < pages
                ? button(HyperionMessages.LIST__NEXT, page + 1)
                : HyperionMessages.LIST__NEXT_NONE.component();

        return HyperionMessages.LIST__FOOTER.component(
                value("previous", previous),
                value("next", next),
                value("page", page),
                value("pages", pages)
        );
    }

    /**
     * Write a button that opens another page.
     *
     * @param label the button's text
     * @param page the page it opens
     * @return the button
     */
    private static Component button(final HyperionMessages label, final int page) {
        Component hover = HyperionMessages.LIST__PAGE_HOVER.component(value("page", page));

        return CommandText.clickable(label.component(), "/hyperion effect list --page " + page, hover);
    }
}
