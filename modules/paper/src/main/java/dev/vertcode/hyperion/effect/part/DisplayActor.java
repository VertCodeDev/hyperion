package dev.vertcode.hyperion.effect.part;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.render.PacketMarker;
import dev.vertcode.hyperion.render.PropData;

/**
 * A prop: a display entity only the effect's viewers know about, at its part's spot, turned and
 * scaled by its keyframes.
 *
 * <p>The display stands at the spot and follows it with smoothed teleports. Its rotation and
 * scale go in its transformation, sent at most every {@link #STEP} ticks and smoothed over them by
 * the client, so an eased move costs a packet every other tick and a still prop costs nothing.
 *
 * <p>Values every prop takes: {@code rotation} in degrees on top of its spot's facing,
 * {@code scale} as a number or {@code [x, y, z]}, {@code visible}, {@code billboard},
 * {@code brightness} (0 to 15), {@code view-range} and {@code glow-color}.
 */
abstract class DisplayActor implements EffectActor {

    /**
     * How many ticks pass at least between two transformation changes, and how long the client
     * smooths each.
     */
    static final int STEP = 2;

    private static final float DEFAULT_VIEW_RANGE = 1f;
    private static final float EPSILON = 1.0e-4f;

    protected final EffectContext context;
    private final PacketMarker marker;
    private @Nullable Location here;
    private @Nullable Quaternionf sentRotation;
    private @Nullable Vector3f sentScale;
    private @Nullable String sentContent;
    private int sentAt = Integer.MIN_VALUE;

    /**
     * Create a prop.
     *
     * @param context the effect it plays in
     * @param type the display's entity type
     */
    DisplayActor(final EffectContext context, final EntityType type) {
        this.context = context;
        this.marker = new PacketMarker(type);
    }

    /**
     * Add what the display shows: its text, item or block.
     *
     * @param into the list to add to
     * @param frame the part's values
     */
    protected abstract void content(List<EntityData<?>> into, EffectFrame frame);

    /**
     * Return a key for what the display shows, so a change of it is sent once.
     *
     * @param frame the part's values
     * @return the key
     */
    protected abstract String contentKey(EffectFrame frame);

    /**
     * Return the offset that puts the display's middle on its spot, for displays drawn from a
     * corner.
     *
     * @param rotation the display's rotation
     * @param scale its scale
     * @return the offset, nothing for displays drawn around their middle
     */
    protected Vector3f centre(final Quaternionf rotation, final Vector3f scale) {
        return new Vector3f();
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        Location at = this.context.location(frame);
        this.here = at;
        if (at == null || !frame.bool("visible", true)) {
            this.marker.hideAll();
            this.sentContent = null;
            return;
        }

        Quaternionf rotation = this.context.rotation(frame).mul(euler(frame.vector("rotation")));
        Vector3f scale = scale(frame);
        Vector3f translation = this.centre(rotation, scale);
        List<User> users = users(this.context.viewers());
        this.marker.place(at, users, () -> this.spawnData(frame, translation, rotation, scale));

        boolean moved = this.sentRotation == null
                || this.sentScale == null
                || !this.sentRotation.equals(rotation, EPSILON)
                || !this.sentScale.equals(scale, EPSILON);
        if (moved && tick - this.sentAt >= STEP && this.marker.shown()) {
            List<EntityData<?>> data = new ArrayList<>();
            PropData.transform(data, translation, rotation, scale, STEP);
            this.marker.update(data);
            this.sentAt = tick;
            this.sentRotation = rotation;
            this.sentScale = scale;
        }

        String content = this.contentKey(frame);
        if (!content.equals(this.sentContent) && this.marker.shown()) {
            List<EntityData<?>> data = new ArrayList<>();
            this.content(data, frame);
            this.marker.update(data);
            this.sentContent = content;
        }
    }

    /**
     * Build everything a fresh display needs, with its current state.
     *
     * @param frame the part's values
     * @param translation its offset from its spot
     * @param rotation its rotation
     * @param scale its scale
     * @return the data
     */
    private List<EntityData<?>> spawnData(
            final EffectFrame frame,
            final Vector3f translation,
            final Quaternionf rotation,
            final Vector3f scale
    ) {
        List<EntityData<?>> data = new ArrayList<>();
        PropData.base(
                data,
                STEP,
                billboard(frame.string("billboard", "fixed")),
                brightness(frame),
                frame.number("view-range", DEFAULT_VIEW_RANGE),
                frame.string("glow-color").map(ParticleData::color).orElse(frame.bool("glow", false) ? Color.WHITE : null)
        );
        PropData.transform(data, translation, rotation, scale, 0);
        this.content(data, frame);
        this.sentContent = this.contentKey(frame);
        if (this.sentRotation == null) {
            this.sentRotation = rotation;
            this.sentScale = scale;
        }

        return data;
    }

    @Override
    public @Nullable Location location() {
        Location at = this.here;

        return at == null ? null : at.clone();
    }

    @Override
    public void stop() {
        this.marker.hideAll();
    }

    /**
     * Turn degrees around X, Y and Z into a rotation, in Blockbench's order.
     *
     * @param degrees the angles, or null for none
     * @return the rotation
     */
    static Quaternionf euler(final @Nullable Vector3f degrees) {
        if (degrees == null) {
            return new Quaternionf();
        }

        return new Quaternionf().rotationZYX(
                (float) Math.toRadians(degrees.z),
                (float) Math.toRadians(degrees.y),
                (float) Math.toRadians(degrees.x)
        );
    }

    /**
     * Read a scale: a number for every axis, or one per axis.
     *
     * @param frame the part's values
     * @return the scale
     */
    static Vector3f scale(final EffectFrame frame) {
        Vector3f each = frame.vector("scale");

        return each != null ? each : new Vector3f(frame.number("scale", 1f));
    }

    /**
     * Find the connections of some players.
     *
     * @param players the players
     * @return their connections, leaving out any PacketEvents does not know yet
     */
    static List<User> users(final List<Player> players) {
        List<User> users = new ArrayList<>(players.size());
        for (Player player : players) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null) {
                users.add(user);
            }
        }

        return users;
    }

    /**
     * Read a billboard by name.
     *
     * @param name the name, any case
     * @return the billboard, fixed for an unknown name
     */
    private static Display.Billboard billboard(final String name) {
        try {
            return Display.Billboard.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Display.Billboard.FIXED;
        }
    }

    /**
     * Read a fixed light level.
     *
     * @param frame the part's values
     * @return the light, or null for the world's
     */
    private static Display.@Nullable Brightness brightness(final EffectFrame frame) {
        if (!frame.has("brightness")) {
            return null;
        }

        int level = Math.clamp(Math.round(frame.number("brightness", 15f)), 0, 15);

        return new Display.Brightness(level, level);
    }
}
