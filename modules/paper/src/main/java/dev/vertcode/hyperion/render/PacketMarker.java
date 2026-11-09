package dev.vertcode.hyperion.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

/**
 * An entity only a model's viewers know about, placed at a spot in the world every frame, such as
 * the interaction of a hitbox or the text display of a name tag.
 *
 * <p>It spawns for each viewer the first frame they are given, moves only when its spot changed,
 * and forgets viewers that are no longer given, whose client has dropped it. Only the thread that
 * owns the model touches it.
 */
public final class PacketMarker {

    private static final double MOVE_EPSILON = 1.0e-6d;

    private final int entityId = SpigotReflectionUtil.generateEntityId();
    private final UUID uniqueId = UUID.randomUUID();
    private final EntityType type;
    private final Map<UUID, User> shown = new HashMap<>();
    private @Nullable Location at;

    /**
     * Create a marker with a fresh entity id. Nothing is sent until {@link #place}.
     *
     * @param type the entity's type
     */
    public PacketMarker(final EntityType type) {
        this.type = type;
    }

    /**
     * Return the marker's entity id, which clicks on it name.
     *
     * @return the id
     */
    public int entityId() {
        return this.entityId;
    }

    /**
     * Put the marker at a spot for every viewer: spawn it for those who do not have it yet, and
     * move it for the rest when the spot changed.
     *
     * @param where the spot
     * @param viewers everyone who should see it now
     * @param spawnData the data a fresh marker needs, asked only when someone is new
     */
    public void place(final Location where, final List<User> viewers, final Supplier<List<EntityData<?>>> spawnData) {
        this.place(where, viewers, Set.of(), spawnData);
    }

    /**
     * Put the marker at a spot for every viewer but some, who have it removed.
     *
     * @param where the spot
     * @param viewers everyone who sees the model now
     * @param left the players among them who should not have the marker
     * @param spawnData the data a fresh marker needs, asked only when someone is new
     */
    public void place(
            final Location where,
            final List<User> viewers,
            final Set<UUID> left,
            final Supplier<List<EntityData<?>>> spawnData
    ) {
        for (UUID player : left) {
            this.hide(player);
        }

        Location last = this.at;
        boolean moved = last == null
                || !Objects.equals(last.getWorld(), where.getWorld())
                || last.distanceSquared(where) > MOVE_EPSILON;
        Set<UUID> current = new HashSet<>();
        List<EntityData<?>> data = null;
        for (User user : viewers) {
            UUID id = user.getUUID();
            if (id == null || left.contains(id)) {
                continue;
            }

            current.add(id);
            if (this.shown.putIfAbsent(id, user) == null) {
                if (data == null) {
                    data = spawnData.get();
                }

                Packets.send(user, Packets.spawn(this.entityId, this.uniqueId, this.type, where));
                Packets.send(user, Packets.metadata(this.entityId, data));
            } else if (moved) {
                Packets.send(user, Packets.teleport(this.entityId, where));
            }
        }

        this.shown.keySet().retainAll(current);
        this.at = where.clone();
    }

    /**
     * Send a change of the marker's data to everyone who has it.
     *
     * @param data the entries that changed
     */
    public void update(final List<EntityData<?>> data) {
        for (User user : this.shown.values()) {
            Packets.send(user, Packets.metadata(this.entityId, data));
        }
    }

    /**
     * Remove the marker from one player.
     *
     * @param player the player's id
     */
    public void hide(final UUID player) {
        User user = this.shown.remove(player);
        if (user != null) {
            Packets.send(user, Packets.destroy(new int[] {this.entityId}));
        }
    }

    /**
     * Remove the marker from everyone who has it.
     */
    public void hideAll() {
        for (User user : this.shown.values()) {
            Packets.send(user, Packets.destroy(new int[] {this.entityId}));
        }

        this.shown.clear();
        this.at = null;
    }

    /**
     * Return whether anyone has the marker.
     *
     * @return true when it is shown to at least one player
     */
    public boolean shown() {
        return !this.shown.isEmpty();
    }
}
