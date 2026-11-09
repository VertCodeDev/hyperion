package dev.vertcode.hyperion.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.behavior.builtin.RiderInput;

/**
 * What the packet listeners need to find quickly: the hitbox behind an interaction's id, the
 * seat behind a rideable entity's id, and the keys each rider holds.
 *
 * <p>Safe from any thread: the listeners read it on the network threads while models write it on
 * their own.
 */
public final class ModelInteractions {

    private final Map<Integer, HitboxTarget> hitboxes = new ConcurrentHashMap<>();
    private final Map<Integer, SeatEntry> seats = new ConcurrentHashMap<>();
    private final Map<UUID, RiderInput> inputs = new ConcurrentHashMap<>();

    /**
     * Note the hitbox an interaction belongs to.
     *
     * @param entityId the interaction's entity id
     * @param target the hitbox
     */
    public void hitbox(final int entityId, final HitboxTarget target) {
        this.hitboxes.put(entityId, target);
    }

    /**
     * Find the hitbox behind an interaction.
     *
     * @param entityId the entity id a player hit or clicked
     * @return the hitbox, or null when the id is not one
     */
    public @Nullable HitboxTarget hitbox(final int entityId) {
        return this.hitboxes.get(entityId);
    }

    /**
     * Forget an interaction.
     *
     * @param entityId the interaction's entity id
     */
    public void forgetHitbox(final int entityId) {
        this.hitboxes.remove(entityId);
    }

    /**
     * Note the seat a rideable entity belongs to.
     *
     * @param entityId the entity's id
     * @param entry the seat
     */
    public void seat(final int entityId, final SeatEntry entry) {
        this.seats.put(entityId, entry);
    }

    /**
     * Find the seat behind a rideable entity.
     *
     * @param entityId the entity's id
     * @return the seat, or null when the entity is not one
     */
    public @Nullable SeatEntry seat(final int entityId) {
        return this.seats.get(entityId);
    }

    /**
     * Return whether an entity is the rideable entity of a seat.
     *
     * @param entityId the entity's id
     * @return true for a seat
     */
    public boolean isSeat(final int entityId) {
        return this.seats.containsKey(entityId);
    }

    /**
     * Forget a seat's rideable entity.
     *
     * @param entityId the entity's id
     */
    public void forgetSeat(final int entityId) {
        this.seats.remove(entityId);
    }

    /**
     * Return the keys a player held when their client last said.
     *
     * @param player the player's id
     * @return the keys, {@link RiderInput#NONE} before the first report
     */
    public RiderInput input(final UUID player) {
        return this.inputs.getOrDefault(player, RiderInput.NONE);
    }

    /**
     * Note the keys a player holds.
     *
     * @param player the player's id
     * @param input the keys
     */
    public void input(final UUID player, final RiderInput input) {
        this.inputs.put(player, input);
    }

    /**
     * Forget a player who left.
     *
     * @param player the player's id
     */
    public void forget(final UUID player) {
        this.inputs.remove(player);
    }
}
