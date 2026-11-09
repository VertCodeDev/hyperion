package dev.vertcode.hyperion.effect.part;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.render.Packets;

/**
 * A camera: the players a play names see through a display only they know about, which moves
 * along the part's keyframes and looks at what {@code look} says.
 *
 * <p>The player stays where they stand, in their own gamemode, held still by {@link CameraLock}.
 * The view goes back when the camera hides, the effect ends, the player sneaks with
 * {@code skippable}, or they die, change worlds or leave.
 *
 * <p>Values: {@code offset} for where the camera is, {@code look} for what it looks at
 * ({@code anchor}, {@code bone:<name>}, {@code locator:<name>}, a part, or {@code [x, y, z]} from
 * the anchor), {@code skippable} and {@code safe}.
 */
final class CameraActor implements EffectActor {

    private static final int SMOOTHING = 3;
    private static final int TELEPORT_DURATION = 10;
    private static final double TOO_CLOSE = 1.0e-6d;

    private final EffectContext context;
    private final CameraLock lock;
    private final Map<UUID, Shot> shots = new ConcurrentHashMap<>();
    private final Set<UUID> done = ConcurrentHashMap.newKeySet();
    private boolean finished;

    /**
     * Create the camera of one part.
     *
     * @param context the effect it plays in
     * @param lock the players cameras hold
     */
    CameraActor(final EffectContext context, final CameraLock lock) {
        this.context = context;
        this.lock = lock;
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        if (this.finished) {
            return;
        }

        Location at = this.context.location(frame);
        if (at == null || !frame.bool("visible", true)) {
            this.finished = true;
            this.stop();
            return;
        }

        Location target = this.lookAt(frame);
        Vector towards = target == null ? null : target.toVector().subtract(at.toVector());
        if (towards != null && towards.lengthSquared() > TOO_CLOSE) {
            at.setDirection(towards);
        }

        boolean skippable = frame.bool("skippable", false);
        boolean safe = frame.bool("safe", true);
        for (Player player : this.context.cameraViewers()) {
            if (!this.shots.containsKey(player.getUniqueId()) && !this.done.contains(player.getUniqueId())) {
                this.take(player, at, safe);
            }
        }

        for (Shot shot : List.copyOf(this.shots.values())) {
            if (!shot.player().isOnline()) {
                this.shots.remove(shot.player().getUniqueId());
                this.lock.forget(shot.player().getUniqueId());
            } else if (skippable && shot.player().isSneaking()) {
                this.release(shot);
            } else {
                Packets.send(shot.user(), Packets.teleportFacing(shot.entityId(), at));
            }
        }
    }

    /**
     * Find what the camera looks at.
     *
     * @param frame the part's values
     * @return the spot, or null to keep the camera's facing
     */
    private @Nullable Location lookAt(final EffectFrame frame) {
        Vector3f offset = frame.vector("look");
        if (offset != null) {
            EffectFrame fromAnchor = new EffectFrame(Map.of("at", "anchor", "offset", offset));
            return this.context.location(fromAnchor);
        }

        String target = frame.string("look").orElse(null);

        return target == null ? null : this.context.location(new EffectFrame(Map.of("at", target)));
    }

    /**
     * Take a player's view.
     *
     * @param player the player
     * @param at where the camera starts
     * @param safe whether they take no damage meanwhile
     */
    private void take(final Player player, final Location at, final boolean safe) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            return;
        }

        Shot shot = new Shot(player, user, SpigotReflectionUtil.generateEntityId());
        this.shots.put(player.getUniqueId(), shot);
        Packets.send(user, Packets.spawn(shot.entityId(), UUID.randomUUID(), EntityTypes.ITEM_DISPLAY, at));
        Packets.send(user, Packets.metadata(shot.entityId(), List.of(
                new EntityData<>(TELEPORT_DURATION, EntityDataTypes.INT, SMOOTHING)
        )));
        Packets.send(user, Packets.teleportFacing(shot.entityId(), at));
        Packets.send(user, Packets.camera(shot.entityId()));
        this.lock.lock(player, safe, () -> this.release(shot));
    }

    /**
     * Give a player their view back.
     *
     * @param shot the player's camera
     */
    private void release(final Shot shot) {
        if (this.shots.remove(shot.player().getUniqueId()) == null) {
            return;
        }

        this.done.add(shot.player().getUniqueId());
        if (shot.player().isOnline()) {
            Packets.send(shot.user(), Packets.camera(shot.player().getEntityId()));
            Packets.send(shot.user(), Packets.destroy(new int[] {shot.entityId()}));
            this.lock.unlock(shot.player());
        } else {
            this.lock.forget(shot.player().getUniqueId());
        }
    }

    @Override
    public void stop() {
        for (Shot shot : List.copyOf(this.shots.values())) {
            this.release(shot);
        }
    }

    /**
     * One player's camera.
     *
     * @param player the player
     * @param user their connection
     * @param entityId the display they see through
     */
    private record Shot(Player player, User user, int entityId) {
    }
}
