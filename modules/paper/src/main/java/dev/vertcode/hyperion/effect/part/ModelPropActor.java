package dev.vertcode.hyperion.effect.part;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.view.ViewerPolicy;
import dev.vertcode.hyperion.blueprint.Blueprint;

/**
 * A Hyperion model as a prop: spawned on its own when the part first shows, seen only by the
 * effect's viewers, moved and scaled with the part, and removed when the part hides or the effect
 * ends.
 *
 * <p>Values: {@code model}, the model's key, {@code animation}, played once it spawns,
 * {@code scale} and {@code rotation}, of which the Y angle turns it.
 */
final class ModelPropActor implements EffectActor {

    private static final int VIEWER_CHECK_TICKS = 10;
    private static final double MOVE_EPSILON = 1.0e-4d;

    private final EffectContext context;
    private final PartServices services;
    private @Nullable Model model;
    private boolean failed;
    private @Nullable Location here;
    private float scale = 1f;
    private Set<UUID> viewers = Set.of();

    /**
     * Create a model prop.
     *
     * @param context the effect it plays in
     * @param services the models
     */
    ModelPropActor(final EffectContext context, final PartServices services) {
        this.context = context;
        this.services = services;
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        Location at = this.context.location(frame);
        if (at == null || !frame.bool("visible", true)) {
            this.remove();
            return;
        }

        at.setYaw(yaw(this.context.rotation(frame).transform(new Vector3f(0f, 0f, 1f))) + rotationY(frame));
        at.setPitch(0f);
        Location before = this.here;
        this.here = at;
        float nextScale = Math.max(0.01f, frame.number("scale", 1f));
        Model shown = this.model;
        if (shown == null || shown.isRemoved()) {
            this.spawn(at, nextScale, frame);
            return;
        }

        boolean moved = before == null || before.distanceSquared(at) > MOVE_EPSILON || before.getYaw() != at.getYaw();
        if (moved && Bukkit.isOwnedByCurrentRegion(at)) {
            shown.teleport(at);
        }

        if (nextScale != this.scale) {
            shown.scale(nextScale);
            this.scale = nextScale;
        }

        if (tick % VIEWER_CHECK_TICKS == 0) {
            Set<UUID> now = ids(this.context.viewers());
            if (!now.equals(this.viewers)) {
                shown.viewers().policy(ViewerPolicy.only(now));
                this.viewers = now;
            }
        }
    }

    /**
     * Spawn the model the part names, once, on the region of its spot.
     *
     * @param at where
     * @param scale how big
     * @param frame the part's values
     */
    private void spawn(final Location at, final float scale, final EffectFrame frame) {
        if (this.failed || !Bukkit.isOwnedByCurrentRegion(at)) {
            return;
        }

        Blueprint blueprint = this.blueprint(frame.string("model", ""));
        if (blueprint == null) {
            this.failed = true;
            return;
        }

        Set<UUID> ids = ids(this.context.viewers());
        String animation = frame.string("animation").orElse(null);
        try {
            this.model = blueprint.spawn(at, options -> {
                options.viewers(ViewerPolicy.only(ids)).scale(scale);
                if (animation != null && blueprint.animations().contains(animation)) {
                    options.animation(animation);
                }
            });
            this.scale = scale;
            this.viewers = ids;
        } catch (RuntimeException exception) {
            this.failed = true;
        }
    }

    /**
     * Find a loaded model by its key, in Hyperion's namespace when it has none.
     *
     * @param name the key, as written
     * @return the model, or null when none is loaded by that key
     */
    private @Nullable Blueprint blueprint(final String name) {
        String lowered = name.trim().toLowerCase(Locale.ROOT);
        try {
            Key key = lowered.contains(":") ? Key.key(lowered) : Key.key("hyperion", lowered);
            return this.services.blueprints().find(key).orElse(null);
        } catch (InvalidKeyException exception) {
            return null;
        }
    }

    /**
     * Remove the model, from any thread.
     */
    private void remove() {
        Model shown = this.model;
        this.model = null;
        if (shown != null && !shown.isRemoved()) {
            this.services.models().remove(shown, RemovalReason.REMOVED);
        }
    }

    @Override
    public @Nullable Location location() {
        Location at = this.here;

        return at == null ? null : at.clone();
    }

    @Override
    public void stop() {
        this.remove();
    }

    /**
     * Return the yaw a direction faces.
     *
     * @param forward the direction
     * @return the yaw in degrees
     */
    private static float yaw(final Vector3f forward) {
        return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
    }

    /**
     * Return the part's own turn around Y.
     *
     * @param frame the part's values
     * @return the angle in degrees
     */
    private static float rotationY(final EffectFrame frame) {
        Vector3f rotation = frame.vector("rotation");

        return rotation == null ? 0f : rotation.y;
    }

    /**
     * Return the ids of some players.
     *
     * @param players the players
     * @return their ids
     */
    private static Set<UUID> ids(final List<Player> players) {
        Set<UUID> ids = new HashSet<>();
        for (Player player : players) {
            ids.add(player.getUniqueId());
        }

        return Set.copyOf(ids);
    }
}
