package dev.vertcode.hyperion.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.config.DebugSettings;

/**
 * Who has which debug view on, and drawing them for models and effects.
 *
 * <p>Models and effects call in from their own frames, so a bone's spot is only read on the
 * thread that owns it. With nobody debugging, a call is one map check. Toggles come from any
 * thread.
 */
public final class DebugViews {

    private static final Set<DebugView> MODEL_VIEWS = EnumSet.of(
            DebugView.BONES,
            DebugView.HITBOXES,
            DebugView.LOCATORS,
            DebugView.SEATS
    );
    private static final Set<DebugView> EFFECT_VIEWS = EnumSet.of(DebugView.EFFECTS);

    private final Map<UUID, Set<DebugView>> enabled = new ConcurrentHashMap<>();
    private volatile DebugSettings settings;

    /**
     * Create the views, all off.
     *
     * @param settings the {@code debug} section of {@code config.yml}
     */
    public DebugViews(final DebugSettings settings) {
        this.settings = settings;
    }

    /**
     * Use new settings from {@code config.yml}.
     *
     * @param settings the {@code debug} section
     */
    public void configure(final DebugSettings settings) {
        this.settings = settings;
    }

    // Toggles

    /**
     * Turn a view on for a player, or off when it was on.
     *
     * @param player the player's id
     * @param view the view
     * @return true when it is on now
     */
    public boolean toggle(final UUID player, final DebugView view) {
        Set<DebugView> now = this.enabled.compute(player, (id, old) -> {
            Set<DebugView> next = old == null ? EnumSet.noneOf(DebugView.class) : EnumSet.copyOf(old);
            if (!next.remove(view)) {
                next.add(view);
            }

            return next.isEmpty() ? null : Set.copyOf(next);
        });

        return now != null && now.contains(view);
    }

    /**
     * Turn every view on for a player.
     *
     * @param player the player's id
     */
    public void showAll(final UUID player) {
        this.enabled.put(player, Set.copyOf(EnumSet.allOf(DebugView.class)));
    }

    /**
     * Turn every view off for a player, as when they leave.
     *
     * @param player the player's id
     */
    public void hideAll(final UUID player) {
        this.enabled.remove(player);
    }

    /**
     * Return the views a player has on.
     *
     * @param player the player's id
     * @return the views, empty when none
     */
    public Set<DebugView> views(final UUID player) {
        return this.enabled.getOrDefault(player, Set.of());
    }

    /**
     * Return whether anyone has a view on, so a frame with no labels up can skip drawing.
     *
     * @return true when someone debugs
     */
    public boolean watching() {
        return !this.enabled.isEmpty();
    }

    // Drawing

    /**
     * Draw a model's views for its viewers who have them on. Call from the model's frame.
     *
     * @param model the model
     * @param box the box around the whole model when it has one, in the world
     * @param viewers the Java players who see the model
     * @param tick the model's frame count
     * @param labels the model's labels from the last call
     * @return the labels to pass next time
     */
    public @Nullable DebugLabels drawModel(
            final Model model,
            final @Nullable BoundingBox box,
            final Collection<Player> viewers,
            final long tick,
            final @Nullable DebugLabels labels
    ) {
        return this.draw(viewers, MODEL_VIEWS, tick, labels, (watching, using) ->
                DebugDraw.model(model, box, watching, using, this.settings));
    }

    /**
     * Draw a running effect for its viewers who have the effects view on. Call from the
     * effect's tick.
     *
     * @param effect builds what to draw, asked only when someone watches
     * @param viewers the players who see the effect
     * @param tick the effect's tick
     * @param labels the effect's labels from the last call
     * @return the labels to pass next time
     */
    public @Nullable DebugLabels drawEffect(
            final Supplier<DebugEffect> effect,
            final Collection<Player> viewers,
            final long tick,
            final @Nullable DebugLabels labels
    ) {
        return this.draw(viewers, EFFECT_VIEWS, tick, labels, (watching, using) ->
                DebugDraw.effect(effect.get(), watching, using));
    }

    /**
     * Draw on the ticks the interval falls on, for the viewers with a wanted view on, and take
     * the labels down once nobody watches.
     */
    private @Nullable DebugLabels draw(
            final Collection<Player> viewers,
            final Set<DebugView> wanted,
            final long tick,
            final @Nullable DebugLabels labels,
            final Drawing drawing
    ) {
        if (labels == null && this.enabled.isEmpty()) {
            return null;
        }

        DebugSettings now = this.settings;
        if (tick % now.interval() != 0) {
            return labels;
        }

        List<Debugger> watching = this.debuggers(viewers, wanted);
        if (watching.isEmpty() || !now.labels()) {
            if (labels != null) {
                labels.clear();
            }

            if (!watching.isEmpty()) {
                drawing.draw(watching, null);
            }

            return null;
        }

        DebugLabels using = labels == null ? new DebugLabels(now.interval()) : labels;
        drawing.draw(watching, using);

        return using.finish() ? using : null;
    }

    /**
     * Find the viewers with any of some views on.
     *
     * @param viewers the players
     * @param wanted the views
     * @return the debuggers among them
     */
    private List<Debugger> debuggers(final Collection<Player> viewers, final Set<DebugView> wanted) {
        List<Debugger> found = new ArrayList<>();
        for (Player player : viewers) {
            Set<DebugView> views = this.enabled.get(player.getUniqueId());
            if (views == null || views.stream().noneMatch(wanted::contains)) {
                continue;
            }

            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null) {
                found.add(new Debugger(player, user, views));
            }
        }

        return found;
    }

    /**
     * Draws one model or effect.
     */
    @FunctionalInterface
    private interface Drawing {

        /**
         * Draw.
         *
         * @param watching the debuggers
         * @param labels where names go, or null without labels
         */
        void draw(List<Debugger> watching, @Nullable DebugLabels labels);
    }
}
