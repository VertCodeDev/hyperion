package dev.vertcode.hyperion.behavior;

import java.util.Objects;

import net.kyori.adventure.key.Key;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.behavior.builtin.HeadBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;

/**
 * A head that turns with where its entity looks, or towards a point.
 *
 * <p>A modifier on the bone adds the turn on top of the animation every frame. On a model on a
 * living entity the bone follows the entity's head relative to its body; with a target it looks
 * at the target, which also works for a model on its own. Anything else leaves the bone to its
 * animation.
 */
final class HyperionHead implements HeadBehavior {

    private static final Key MODIFIER = Key.key("hyperion", "head");
    private static final float MAX_YAW = 180f;
    private static final float MAX_PITCH = 90f;
    private static final double TOO_CLOSE = 1.0e-6d;

    private final Bone bone;
    private volatile boolean enabled = true;
    private volatile float maxYaw;
    private volatile float maxPitch;
    private volatile @Nullable Location target;

    /**
     * Create the head of a bone.
     *
     * @param bone the bone
     * @param maxYaw how far it turns left or right from the body, in degrees
     * @param maxPitch how far it looks up or down, in degrees
     */
    HyperionHead(final Bone bone, final float maxYaw, final float maxPitch) {
        this.bone = bone;
        this.maxYaw = maxYaw;
        this.maxPitch = maxPitch;
    }

    @Override
    public void onSpawn() {
        this.bone.modifier(MODIFIER, (moved, transform) -> this.turn(transform));
    }

    @Override
    public void onRemove(final RemovalReason reason) {
        this.bone.removeModifier(MODIFIER);
    }

    /**
     * Add this frame's turn to the bone's transform.
     *
     * @param transform the transform so far
     * @return the transform with the turn
     */
    private BoneTransform turn(final BoneTransform transform) {
        if (!this.enabled) {
            return transform;
        }

        Model model = this.bone.model();
        Location aim = this.target;
        float yaw;
        float pitch;
        if (aim != null) {
            Location from = this.bone.location();
            Vector towards = aim.toVector().subtract(from.toVector());
            if (!Objects.equals(aim.getWorld(), from.getWorld()) || towards.lengthSquared() < TOO_CLOSE) {
                return transform;
            }

            float aimYaw = (float) Math.toDegrees(Math.atan2(-towards.getX(), towards.getZ()));
            yaw = aimYaw - model.rotation().yaw();
            pitch = (float) -Math.toDegrees(Math.atan2(towards.getY(), Math.hypot(towards.getX(), towards.getZ())));
        } else if (model.anchor() instanceof EntityAnchor(Entity entity) && entity instanceof LivingEntity living) {
            // A living entity's yaw is where its head looks; the body turns on its own.
            yaw = living.getYaw() - living.getBodyYaw();
            pitch = living.getPitch();
        } else {
            return transform;
        }

        float limitYaw = this.maxYaw;
        float limitPitch = this.maxPitch;
        float turnYaw = Math.clamp(wrap(yaw), -limitYaw, limitYaw);
        float turnPitch = Math.clamp(pitch, -limitPitch, limitPitch);

        // Bones face north, so a turn to the right is a negative turn around Y and looking down
        // a negative turn around X.
        Quaternionf look = new Quaternionf()
                .rotationY((float) Math.toRadians(-turnYaw))
                .rotateX((float) Math.toRadians(-turnPitch));

        return transform.withRotation(look.mul(transform.rotation()));
    }

    /**
     * Bring an angle into -180 to 180 degrees.
     *
     * @param degrees the angle
     * @return the same angle, wrapped
     */
    private static float wrap(final float degrees) {
        float wrapped = degrees % 360f;
        if (wrapped >= 180f) {
            wrapped -= 360f;
        } else if (wrapped < -180f) {
            wrapped += 360f;
        }

        return wrapped;
    }

    @Override
    public boolean enabled() {
        return this.enabled;
    }

    @Override
    public HeadBehavior enabled(final boolean enabled) {
        this.enabled = enabled;

        return this;
    }

    @Override
    public float maxYaw() {
        return this.maxYaw;
    }

    @Override
    public HeadBehavior maxYaw(final float degrees) {
        this.maxYaw = Math.clamp(degrees, 0f, MAX_YAW);

        return this;
    }

    @Override
    public float maxPitch() {
        return this.maxPitch;
    }

    @Override
    public HeadBehavior maxPitch(final float degrees) {
        this.maxPitch = Math.clamp(degrees, 0f, MAX_PITCH);

        return this;
    }

    @Override
    public @Nullable Location target() {
        Location aim = this.target;

        return aim == null ? null : aim.clone();
    }

    @Override
    public HeadBehavior target(final @Nullable Location target) {
        this.target = target == null ? null : target.clone();

        return this;
    }
}
