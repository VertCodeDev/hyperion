package dev.vertcode.hyperion.ui;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.view.ViewerPolicy;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.config.GallerySettings;
import dev.vertcode.hyperion.model.ModelManager;

/**
 * The private copies the gallery shows: one per player, in front of them, seen by them alone.
 *
 * <p>A new preview replaces the old one. A preview goes after {@code gallery.preview-seconds},
 * when its player leaves, or when they remove it.
 */
public final class Previews {

    private static final double DISTANCE = 3d;
    private static final long TICKS_PER_SECOND = 20L;
    private static final double LEVEL = 1.0e-6d;

    private final Plugin plugin;
    private final ModelManager models;
    private final Map<UUID, Model> shown = new ConcurrentHashMap<>();
    private volatile GallerySettings settings;

    /**
     * Create the previews.
     *
     * @param plugin the plugin, for scheduling
     * @param models the live models, for removing previews from any thread
     * @param settings the {@code gallery} section of {@code config.yml}
     */
    public Previews(final Plugin plugin, final ModelManager models, final GallerySettings settings) {
        this.plugin = plugin;
        this.models = models;
        this.settings = settings;
    }

    /**
     * Use new settings from {@code config.yml}.
     *
     * @param settings the {@code gallery} section
     */
    public void configure(final GallerySettings settings) {
        this.settings = settings;
    }

    /**
     * Return how long a preview stays.
     *
     * @return the seconds
     */
    public int seconds() {
        return this.settings.previewSeconds();
    }

    /**
     * Show a player a preview of a model, in place of the one they had. Call on the player's
     * thread.
     *
     * @param player the player
     * @param blueprint the model
     * @return the preview
     * @throws IllegalStateException if the spot in front of the player belongs to another region
     */
    public Model show(final Player player, final Blueprint blueprint) {
        UUID id = player.getUniqueId();
        this.clear(id);
        Model model = blueprint.spawn(inFront(player), options -> options.viewers(ViewerPolicy.only(Set.of(id))));
        this.shown.put(id, model);
        long ticks = this.settings.previewSeconds() * TICKS_PER_SECOND;
        player.getScheduler().runDelayed(this.plugin, task -> this.expire(id, model), null, ticks);

        return model;
    }

    /**
     * Return a player's preview, when they have one.
     *
     * @param player the player's id
     * @return the preview, or empty
     */
    public Optional<Model> current(final UUID player) {
        Model model = this.shown.get(player);

        return model == null || model.isRemoved() ? Optional.empty() : Optional.of(model);
    }

    /**
     * Remove a player's preview.
     *
     * @param player the player's id
     * @return true when they had one
     */
    public boolean clear(final UUID player) {
        Model model = this.shown.remove(player);
        if (model == null || model.isRemoved()) {
            return false;
        }

        this.models.remove(model, RemovalReason.REMOVED);

        return true;
    }

    /**
     * Remove every preview, as when the plugin stops.
     */
    public void clearAll() {
        for (UUID player : Set.copyOf(this.shown.keySet())) {
            this.clear(player);
        }
    }

    /**
     * Remove a preview whose time is up, unless it was replaced since.
     *
     * @param player the player's id
     * @param model the preview
     */
    private void expire(final UUID player, final Model model) {
        if (this.shown.remove(player, model) && !model.isRemoved()) {
            this.models.remove(model, RemovalReason.REMOVED);
        }
    }

    /**
     * Return the spot a few blocks in front of a player, on their level, facing them.
     *
     * @param player the player
     * @return the spot
     */
    static Location inFront(final Player player) {
        Entity self = player;
        Location at = self.getLocation();
        Vector ahead = at.getDirection().setY(0d);
        if (ahead.lengthSquared() < LEVEL) {
            // Looking straight up or down: go by the yaw alone.
            double yaw = Math.toRadians(at.getYaw());
            ahead = new Vector(-Math.sin(yaw), 0d, Math.cos(yaw));
        }

        Location spot = at.clone().add(ahead.normalize().multiply(DISTANCE));
        spot.setYaw(at.getYaw() + 180f);
        spot.setPitch(0f);

        return spot;
    }
}
