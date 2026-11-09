package dev.vertcode.hyperion.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.view.ViewerPolicy;
import dev.vertcode.hyperion.api.model.view.Viewers;

/**
 * Who sees a model now, and who may.
 *
 * <p>The api methods are safe from any thread: they record the wish and mark the viewers for the
 * next frame, which sends the spawns and removals. The packet listener reads
 * {@link #isViewing(UUID)} from Netty threads.
 */
final class ModelViewers implements Viewers {

    private static final String NOT_POSITIVE = "A view distance must be above 0, got %d";

    private final Map<UUID, Viewer> current = new ConcurrentHashMap<>();
    private final Set<UUID> shown = ConcurrentHashMap.newKeySet();
    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();
    private volatile ViewerPolicy policy;
    private volatile int viewDistance;
    private volatile boolean dirty;

    /**
     * Create the viewers of a model nobody sees yet.
     *
     * @param policy who may see the model
     * @param viewDistance how far away, in blocks, players see it
     */
    ModelViewers(final ViewerPolicy policy, final int viewDistance) {
        this.policy = policy;
        this.viewDistance = viewDistance;
    }

    @Override
    public Collection<Player> current() {
        return this.current.values().stream().map(Viewer::player).toList();
    }

    @Override
    public boolean isViewing(final Player player) {
        return this.current.containsKey(player.getUniqueId());
    }

    /**
     * Return whether a player sees the model now, by id.
     *
     * @param player the player's id
     * @return true when they see it
     */
    boolean isViewing(final UUID player) {
        return this.current.containsKey(player);
    }

    @Override
    public ViewerPolicy policy() {
        return this.policy;
    }

    @Override
    public void policy(final ViewerPolicy policy) {
        this.policy = policy;
        this.dirty = true;
    }

    @Override
    public int viewDistance() {
        return this.viewDistance;
    }

    @Override
    public void viewDistance(final int blocks) {
        if (blocks <= 0) {
            throw new IllegalArgumentException(String.format(NOT_POSITIVE, blocks));
        }

        this.viewDistance = blocks;
        this.dirty = true;
    }

    @Override
    public void show(final Player player) {
        this.hidden.remove(player.getUniqueId());
        this.shown.add(player.getUniqueId());
        this.dirty = true;
    }

    @Override
    public void hide(final Player player) {
        this.shown.remove(player.getUniqueId());
        this.hidden.add(player.getUniqueId());
        this.dirty = true;
    }

    @Override
    public void reset(final Player player) {
        this.shown.remove(player.getUniqueId());
        this.hidden.remove(player.getUniqueId());
        this.dirty = true;
    }

    /**
     * Return whether a player should see the model from where it is: in range, and allowed by a
     * {@link #show} or the policy, with no {@link #hide}.
     *
     * @param player the player
     * @param origin where the model is
     * @return true when they should see it
     */
    boolean wants(final Player player, final Location origin) {
        UUID id = player.getUniqueId();
        if (this.hidden.contains(id) || !player.getWorld().equals(origin.getWorld())) {
            return false;
        }

        // Paper marks Player#getLocation nullable; the Entity method it overrides never returns null.
        Entity self = player;
        double range = this.viewDistance;
        if (self.getLocation().distanceSquared(origin) > range * range) {
            return false;
        }

        return this.shown.contains(id) || this.policy.canSee(player);
    }

    /**
     * Return the viewers with their connections, for sending.
     *
     * @return the viewers
     */
    Collection<Viewer> viewers() {
        return this.current.values();
    }

    /**
     * Return a snapshot of the viewers, safe to change the viewers while walking it.
     *
     * @return the viewers
     */
    List<Viewer> snapshot() {
        return List.copyOf(this.current.values());
    }

    /**
     * Start counting a player as a viewer.
     *
     * @param player the player
     * @param user the player's connection
     * @param bedrock whether the player is on Bedrock
     * @return the viewer
     */
    Viewer add(final Player player, final User user, final boolean bedrock) {
        Viewer viewer = new Viewer(player, user, bedrock);
        this.current.put(player.getUniqueId(), viewer);

        return viewer;
    }

    /**
     * Stop counting a player as a viewer.
     *
     * @param player the player's id
     * @return the viewer, or null when they were not one
     */
    @Nullable Viewer remove(final UUID player) {
        return this.current.remove(player);
    }

    /**
     * Forget everything about a player who left: their view and any {@link #show} or
     * {@link #hide}.
     *
     * @param player the player's id
     */
    void forget(final UUID player) {
        this.current.remove(player);
        this.shown.remove(player);
        this.hidden.remove(player);
    }

    boolean isEmpty() {
        return this.current.isEmpty();
    }

    /**
     * Return whether the viewers changed in a way the next frame must check, and clear the mark.
     *
     * @return true once after every change
     */
    boolean takeDirty() {
        boolean was = this.dirty;
        this.dirty = false;

        return was;
    }
}
