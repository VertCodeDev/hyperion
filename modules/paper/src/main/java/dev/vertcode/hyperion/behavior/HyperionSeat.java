package dev.vertcode.hyperion.behavior;

import java.util.List;
import java.util.Optional;

import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.behavior.builtin.RiderInput;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatController;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.model.FrameBehavior;
import dev.vertcode.hyperion.model.ModelInteractions;
import dev.vertcode.hyperion.model.SeatEntry;
import dev.vertcode.hyperion.render.Packets;

/**
 * A seat: the rider sits on an empty item display that exists only while someone sits.
 *
 * <p>On a model on an entity the display rides that entity, so the server carries the rider
 * along. The model's viewers are told the display rides nothing and see it moved to the seat's
 * bone after every frame, so the rider sits where the model shows the seat, animation included.
 * Everyone else sees the rider on the entity itself.
 *
 * <p>With a controller the rider steers the entity: Hyperion sets its velocity every tick from
 * the rider's keys and holds back its own AI until the rider leaves.
 */
final class HyperionSeat implements SeatBehavior, FrameBehavior {

    private static final int MAX_TELEPORT_DURATION = 59;
    private static final String NOT_OWNER = "Mount and dismount on the thread that owns the model";

    private final Bone bone;
    private final ModelInteractions index;
    private final NamespacedKey seatKey;
    private volatile boolean locked;
    private volatile @Nullable SeatController controller;
    private @Nullable ItemDisplay seat;
    private boolean steering;
    private boolean wasAware;
    private boolean hadGravity;

    /**
     * Create the seat of a bone.
     *
     * @param bone the bone
     * @param index where seats are noted, so the rider's packets are found
     * @param seatKey the mark every seat's display carries
     * @param controller what lets the rider steer, or null to only ride along
     */
    HyperionSeat(
            final Bone bone,
            final ModelInteractions index,
            final NamespacedKey seatKey,
            final @Nullable SeatController controller
    ) {
        this.bone = bone;
        this.index = index;
        this.seatKey = seatKey;
        this.controller = controller;
    }

    @Override
    public Optional<Entity> passenger() {
        ItemDisplay display = this.seat;
        if (display == null) {
            return Optional.empty();
        }

        List<Entity> riders = display.getPassengers();

        return riders.isEmpty() ? Optional.empty() : Optional.of(riders.getFirst());
    }

    @Override
    public boolean mount(final Entity entity) {
        this.requireOwner();
        Entity carrier = this.carrier();
        if (this.seat != null || entity.equals(carrier) || !entity.isValid() || !Bukkit.isOwnedByCurrentRegion(entity)) {
            return false;
        }

        Location at = this.bone.location();
        if (at.getWorld() == null) {
            return false;
        }

        if (entity.isInsideVehicle()) {
            entity.leaveVehicle();
        }

        int smoothing = Math.clamp(this.bone.model().interpolation(), 0, MAX_TELEPORT_DURATION);
        Location spawnAt = carrier != null ? carrier.getLocation() : at;
        ItemDisplay display = at.getWorld().spawn(spawnAt, ItemDisplay.class, spawned -> {
            spawned.setPersistent(false);
            spawned.setTeleportDuration(smoothing);
            spawned.getPersistentDataContainer().set(this.seatKey, PersistentDataType.BYTE, (byte) 1);
        });

        // Noted before anything rides, so the packets that follow already leave the seat out.
        this.index.seat(display.getEntityId(), new SeatEntry(this.bone.model(), this));
        if (carrier != null) {
            carrier.addPassenger(display);
        }

        if (!display.addPassenger(entity)) {
            this.index.forgetSeat(display.getEntityId());
            display.remove();
            return false;
        }

        this.seat = display;

        return true;
    }

    @Override
    public void dismount() {
        this.requireOwner();
        this.release();
    }

    /**
     * Let the rider off and take the seat's display away.
     */
    private void release() {
        ItemDisplay display = this.seat;
        if (display == null) {
            return;
        }

        this.seat = null;
        this.index.forgetSeat(display.getEntityId());
        this.stopSteering();
        display.eject();
        display.remove();
    }

    @Override
    public void onTick() {
        ItemDisplay display = this.seat;
        if (display == null) {
            return;
        }

        List<Entity> riders = display.getPassengers();
        if (!display.isValid() || riders.isEmpty()) {
            this.release();
            return;
        }

        this.steer(riders.getFirst());
    }

    @Override
    public void afterFrame(final List<User> viewers) {
        ItemDisplay display = this.seat;
        if (display == null) {
            return;
        }

        Location at = this.bone.location();
        for (User user : viewers) {
            Packets.send(user, Packets.teleport(display.getEntityId(), at));
        }
    }

    @Override
    public void onRemove(final RemovalReason reason) {
        this.release();
    }

    /**
     * Move the entity carrying the model the way the rider steers it, when they may.
     *
     * @param rider whoever sits in the seat
     */
    private void steer(final Entity rider) {
        SeatController steers = this.controller;
        Entity carrier = this.carrier();
        if (steers == null || carrier == null || !(rider instanceof Player player)) {
            this.stopSteering();
            return;
        }

        if (!this.steering) {
            this.steering = true;
            this.hadGravity = carrier.hasGravity();
            if (carrier instanceof Mob mob) {
                this.wasAware = mob.isAware();
                mob.setAware(false);
            }
        }

        carrier.setGravity(this.hadGravity && !steers.flies());
        RiderInput input = this.index.input(player.getUniqueId());
        Vector velocity = steers.steer(player, carrier, input);
        carrier.setVelocity(velocity);
        if (steers.turnsWithRider()) {
            carrier.setRotation(player.getYaw(), 0f);
            if (carrier instanceof LivingEntity living) {
                living.setBodyYaw(player.getYaw());
            }
        }
    }

    /**
     * Give the entity carrying the model its own AI and gravity back after a rider steered it.
     */
    private void stopSteering() {
        if (!this.steering) {
            return;
        }

        this.steering = false;
        Entity carrier = this.carrier();
        if (carrier == null || !carrier.isValid()) {
            return;
        }

        carrier.setGravity(this.hadGravity);
        if (carrier instanceof Mob mob) {
            mob.setAware(this.wasAware);
        }
    }

    /**
     * Return the entity carrying the model.
     *
     * @return the entity, or null for a model on its own
     */
    private @Nullable Entity carrier() {
        return this.bone.model().anchor() instanceof EntityAnchor(Entity entity) ? entity : null;
    }

    /**
     * Make sure the calling thread owns the model.
     *
     * @throws IllegalStateException if it does not
     */
    private void requireOwner() {
        Entity carrier = this.carrier();
        boolean owned = carrier != null
                ? Bukkit.isOwnedByCurrentRegion(carrier)
                : Bukkit.isOwnedByCurrentRegion(this.bone.model().location());
        if (!owned) {
            throw new IllegalStateException(NOT_OWNER);
        }
    }

    @Override
    public boolean locked() {
        return this.locked;
    }

    @Override
    public SeatBehavior locked(final boolean locked) {
        this.locked = locked;

        return this;
    }

    @Override
    public @Nullable SeatController controller() {
        return this.controller;
    }

    @Override
    public SeatBehavior controller(final @Nullable SeatController controller) {
        this.controller = controller;

        return this;
    }
}
