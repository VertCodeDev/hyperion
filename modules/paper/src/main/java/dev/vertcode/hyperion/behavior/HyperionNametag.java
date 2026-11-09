package dev.vertcode.hyperion.behavior;

import java.util.List;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.RemovalReason;
import dev.vertcode.hyperion.api.model.behavior.builtin.NametagBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.model.FrameBehavior;
import dev.vertcode.hyperion.render.MarkerData;
import dev.vertcode.hyperion.render.PacketMarker;

/**
 * A name over a bone: a text display that only the model's viewers know about, moved to the bone
 * after every frame.
 *
 * <p>Without text of its own it shows the custom name of the entity carrying the model, which
 * the model hides from the entity itself, and nothing while that entity has no name.
 */
final class HyperionNametag implements NametagBehavior, FrameBehavior {

    private final Bone bone;
    private final PacketMarker marker = new PacketMarker(EntityTypes.TEXT_DISPLAY);
    private volatile @Nullable Component text;
    private volatile boolean seeThrough;
    private @Nullable Component sentText;
    private boolean sentSeeThrough;

    /**
     * Create the name tag of a bone.
     *
     * @param bone the bone
     */
    HyperionNametag(final Bone bone) {
        this.bone = bone;
    }

    @Override
    public void afterFrame(final List<User> viewers) {
        Component shown = this.shownText();
        if (shown == null) {
            this.marker.hideAll();
            this.sentText = null;
            return;
        }

        boolean through = this.seeThrough;
        if (this.marker.shown() && (!shown.equals(this.sentText) || through != this.sentSeeThrough)) {
            this.marker.update(MarkerData.text(shown, through));
        }

        this.sentText = shown;
        this.sentSeeThrough = through;
        int interpolation = this.bone.model().interpolation();
        this.marker.place(this.bone.location(), viewers, () -> MarkerData.nametag(shown, through, interpolation));
    }

    /**
     * Work out the text to show now.
     *
     * @return the text, or null to show nothing
     */
    private @Nullable Component shownText() {
        Component own = this.text;
        if (own != null) {
            return own;
        }

        Model model = this.bone.model();
        if (model.anchor() instanceof EntityAnchor(Entity entity) && !(entity instanceof Player)) {
            return entity.customName();
        }

        return null;
    }

    @Override
    public void onHide(final Player viewer) {
        this.marker.hide(viewer.getUniqueId());
    }

    @Override
    public void onRemove(final RemovalReason reason) {
        this.marker.hideAll();
    }

    @Override
    public @Nullable Component text() {
        return this.text;
    }

    @Override
    public NametagBehavior text(final @Nullable Component text) {
        this.text = text;

        return this;
    }

    @Override
    public boolean seeThrough() {
        return this.seeThrough;
    }

    @Override
    public NametagBehavior seeThrough(final boolean seeThrough) {
        this.seeThrough = seeThrough;

        return this;
    }
}
