package dev.vertcode.hyperion.model;

import java.util.List;
import java.util.UUID;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.render.MarkerData;
import dev.vertcode.hyperion.render.PacketMarker;

/**
 * The one hitbox around a whole model: the box around its cubes at rest, turned and scaled with
 * the model.
 *
 * <p>A hitbox cannot turn, so a long model gets a square box as wide as its longest side. Only
 * the thread that owns the model touches it.
 */
final class ModelBox implements HitboxTarget {

    private static final float MIN_SIZE = 0.25f;

    private final HyperionModel model;
    private final ModelInteractions index;
    private final boolean forwards;
    private final PacketMarker marker = new PacketMarker(EntityTypes.INTERACTION);
    private float width;
    private float height;

    /**
     * Create the box and note it, so clicks on it are found.
     *
     * @param model the model
     * @param index where hitboxes are noted
     * @param forwards whether hits pass on to the entity carrying the model
     */
    ModelBox(final HyperionModel model, final ModelInteractions index, final boolean forwards) {
        this.model = model;
        this.index = index;
        this.forwards = forwards;
        index.hitbox(this.marker.entityId(), this);
    }

    /**
     * Put the box around the model for this frame.
     *
     * @param viewers the Java players who see the model
     */
    void place(final List<User> viewers) {
        BoundingBox box = this.model.currentBlueprint().boundingBox();
        float scale = this.model.scale();
        float nextWidth = Math.max(MIN_SIZE, (float) Math.max(box.getWidthX(), box.getWidthZ()) * scale);
        float nextHeight = Math.max(MIN_SIZE, (float) box.getHeight() * scale);
        if ((nextWidth != this.width || nextHeight != this.height) && this.marker.shown()) {
            this.marker.update(MarkerData.interaction(nextWidth, nextHeight));
        }

        this.width = nextWidth;
        this.height = nextHeight;
        Vector3f bottom = this.model.rootWorld().transformPosition(new Vector3f(
                (float) box.getCenterX(),
                (float) box.getMinY(),
                (float) box.getCenterZ()
        ));
        Location at = this.model.location().add(bottom.x, bottom.y, bottom.z);
        this.marker.place(at, viewers, this.riders(), () -> MarkerData.interaction(nextWidth, nextHeight));
    }

    /**
     * Remove the box from one player.
     *
     * @param player the player's id
     */
    void hide(final UUID player) {
        this.marker.hide(player);
    }

    /**
     * Remove the box from everyone and forget it.
     */
    void remove() {
        this.marker.hideAll();
        this.index.forgetHitbox(this.marker.entityId());
    }

    @Override
    public Model model() {
        return this.model;
    }

    @Override
    public @Nullable Bone bone() {
        return null;
    }

    @Override
    public boolean forwardsDamage() {
        return this.forwards;
    }
}
