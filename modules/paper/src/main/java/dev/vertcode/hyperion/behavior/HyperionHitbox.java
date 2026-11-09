package dev.vertcode.hyperion.behavior;

import java.util.List;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.hitbox.Hitbox;
import dev.vertcode.hyperion.api.model.hitbox.HitboxBehavior;
import dev.vertcode.hyperion.api.model.hitbox.HitboxMode;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.model.FrameBehavior;
import dev.vertcode.hyperion.model.HitboxTarget;
import dev.vertcode.hyperion.model.ModelInteractions;
import dev.vertcode.hyperion.render.MarkerData;
import dev.vertcode.hyperion.render.PacketMarker;

/**
 * A hitbox bone: an interaction that only the model's viewers know about, moved to the bone after
 * every frame, which players hit and click.
 *
 * <p>Its size comes from the bone's own cubes until set. It shows only while the model's hitbox
 * mode is {@link HitboxMode#BONES}; the model's box takes over otherwise.
 */
final class HyperionHitbox implements HitboxBehavior, FrameBehavior, HitboxTarget {

    private static final float PIXELS_PER_BLOCK = 16f;
    private static final float FALLBACK_SIZE = 0.5f;

    private final Bone bone;
    private final ModelInteractions index;
    private final PacketMarker marker = new PacketMarker(EntityTypes.INTERACTION);
    private volatile Hitbox hitbox;
    private volatile boolean interactable = true;
    private volatile boolean forwards;
    private float sentWidth;
    private float sentHeight;

    /**
     * Create the hitbox of a bone and note it, so clicks on it are found.
     *
     * @param bone the bone
     * @param index where hitboxes are noted
     * @param forwards whether hits pass on to the entity carrying the model
     */
    HyperionHitbox(final Bone bone, final ModelInteractions index, final boolean forwards) {
        this.bone = bone;
        this.index = index;
        this.forwards = forwards;
        this.hitbox = sizeOf(bone.blueprint());
        index.hitbox(this.marker.entityId(), this);
    }

    /**
     * Work out a hitbox from a bone's own cubes: square from above, as wide as their widest
     * side, standing at the bottom center of the box around them.
     *
     * @param blueprint the bone's blueprint
     * @return the hitbox, half a block when the bone has no cubes
     */
    static Hitbox sizeOf(final BoneBlueprint blueprint) {
        if (!(blueprint instanceof BlueprintBone shape) || shape.cubes().isEmpty()) {
            return Hitbox.of(FALLBACK_SIZE, FALLBACK_SIZE);
        }

        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        for (Cube cube : shape.cubes()) {
            Vector3fc from = cube.from();
            Vector3fc to = cube.to();
            float grow = cube.inflate();
            min.min(new Vector3f(Math.min(from.x(), to.x()) - grow, Math.min(from.y(), to.y()) - grow, Math.min(from.z(), to.z()) - grow));
            max.max(new Vector3f(Math.max(from.x(), to.x()) + grow, Math.max(from.y(), to.y()) + grow, Math.max(from.z(), to.z()) + grow));
        }

        min.div(PIXELS_PER_BLOCK);
        max.div(PIXELS_PER_BLOCK);
        float width = Math.max(max.x - min.x, max.z - min.z);
        float height = max.y - min.y;
        if (width <= 0f || height <= 0f) {
            return Hitbox.of(FALLBACK_SIZE, FALLBACK_SIZE);
        }

        Vector3fc pivot = shape.pivot();
        Vector3f offset = new Vector3f((min.x + max.x) / 2f, min.y, (min.z + max.z) / 2f).sub(pivot);

        return new Hitbox(width, height, offset);
    }

    @Override
    public void afterFrame(final List<User> viewers) {
        Model model = this.bone.model();
        if (!this.interactable || model.hitboxMode() != HitboxMode.BONES) {
            this.marker.hideAll();
            return;
        }

        Hitbox box = this.hitbox;
        float scale = model.scale();
        float width = box.width() * scale;
        float height = box.height() * scale;
        if ((width != this.sentWidth || height != this.sentHeight) && this.marker.shown()) {
            this.marker.update(MarkerData.interaction(width, height));
        }

        this.sentWidth = width;
        this.sentHeight = height;
        Vector3f offset = this.bone.rotation().transform(new Vector3f(box.offset())).mul(scale);
        Location at = this.bone.location().add(offset.x, offset.y, offset.z);
        this.marker.place(at, viewers, this.riders(), () -> MarkerData.interaction(width, height));
    }

    @Override
    public void onHide(final Player viewer) {
        this.marker.hide(viewer.getUniqueId());
    }

    @Override
    public void onRemove(final RemovalReason reason) {
        this.marker.hideAll();
        this.index.forgetHitbox(this.marker.entityId());
    }

    @Override
    public Hitbox hitbox() {
        return this.hitbox;
    }

    @Override
    public HitboxBehavior hitbox(final Hitbox hitbox) {
        this.hitbox = hitbox;

        return this;
    }

    @Override
    public boolean interactable() {
        return this.interactable;
    }

    @Override
    public HitboxBehavior interactable(final boolean interactable) {
        this.interactable = interactable;

        return this;
    }

    @Override
    public boolean forwardsDamage() {
        return this.forwards;
    }

    @Override
    public HitboxBehavior forwardsDamage(final boolean forwards) {
        this.forwards = forwards;

        return this;
    }

    @Override
    public Model model() {
        return this.bone.model();
    }

    @Override
    public Bone bone() {
        return this.bone;
    }
}
