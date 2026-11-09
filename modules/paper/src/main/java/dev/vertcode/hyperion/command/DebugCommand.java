package dev.vertcode.hyperion.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.config.HyperionMessages;
import dev.vertcode.hyperion.debug.DebugView;
import dev.vertcode.hyperion.debug.DebugViews;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * {@code /hyperion debug [view]}: turn a debug view on or off for yourself, or see which are on.
 */
public final class DebugCommand {

    private static final String SUGGESTIONS = "debug-views";
    private static final String ALL = "all";
    private static final String OFF = "off";

    private final DebugViews debug;

    /**
     * Create the command.
     *
     * @param debug the debug views
     */
    public DebugCommand(final DebugViews debug) {
        this.debug = debug;
    }

    /**
     * Turn a view on, or off when it was on; {@code all} and {@code off} switch every view.
     * Without a view, say which are on.
     *
     * @param source the player
     * @param name the view, {@code all} or {@code off}, or null to see which are on
     */
    @Command(HyperionCommands.ROOT + " debug [view]")
    @CommandDescription("Show bones, hitboxes, locators, seats or effects on the models near you")
    @Permission(HyperionPermissions.DEBUG)
    public void debug(
            final PlayerSource source,
            @Argument(value = "view", suggestions = SUGGESTIONS) final @Nullable String name
    ) {
        Player player = source.source();
        UUID id = player.getUniqueId();
        if (name == null) {
            this.status(player);
            return;
        }

        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.equals(ALL)) {
            this.debug.showAll(id);
            HyperionMessages.DEBUG__ALL.send(player);
            return;
        }

        if (key.equals(OFF)) {
            this.debug.hideAll(id);
            HyperionMessages.DEBUG__NONE.send(player);
            return;
        }

        DebugView view = DebugView.fromKey(key).orElse(null);
        if (view == null) {
            HyperionMessages.DEBUG__UNKNOWN.send(player, value("view", name));
            return;
        }

        boolean on = this.debug.toggle(id, view);
        (on ? HyperionMessages.DEBUG__SHOWN : HyperionMessages.DEBUG__HIDDEN).send(player, value("view", view.key()));
    }

    /**
     * Suggest every view, {@code all} and {@code off}.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the names
     */
    @Suggestions(SUGGESTIONS)
    public List<String> suggest(final CommandContext<Source> context, final String input) {
        List<String> names = new ArrayList<>();
        for (DebugView view : DebugView.values()) {
            names.add(view.key());
        }

        names.add(ALL);
        names.add(OFF);

        return names;
    }

    /**
     * Tell a player which views they have on.
     *
     * @param player the player
     */
    private void status(final Player player) {
        Set<DebugView> views = this.debug.views(player.getUniqueId());
        if (views.isEmpty()) {
            HyperionMessages.DEBUG__STATUS_NONE.send(player);
            return;
        }

        StringJoiner names = new StringJoiner(", ");
        for (DebugView view : DebugView.values()) {
            if (views.contains(view)) {
                names.add(view.key());
            }
        }

        HyperionMessages.DEBUG__STATUS.send(player, value("views", names.toString()));
    }
}
