package dev.vertcode.hyperion.render;

import java.util.List;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The entity data of an item display, as sent in metadata packets.
 *
 * <p>The indexes are the ones every version from 1.20.2 on uses. Each method adds the entries of
 * one kind of change to a list, so a frame sends only what changed in one packet per display.
 */
public final class DisplayData {

    /**
     * The entity flags byte every entity has.
     */
    public static final int FLAGS = 0;

    /**
     * The custom name every entity has.
     */
    public static final int CUSTOM_NAME = 2;

    /**
     * Whether the custom name shows without looking at the entity.
     */
    public static final int CUSTOM_NAME_VISIBLE = 3;

    /**
     * The flag for an entity that burns.
     */
    public static final byte ON_FIRE = 0x01;

    /**
     * The flag for an invisible entity.
     */
    public static final byte INVISIBLE = 0x20;

    /**
     * The flag for the glowing outline.
     */
    public static final byte GLOWING = 0x40;

    private static final int INTERPOLATION_DELAY = 8;
    private static final int TRANSFORMATION_DURATION = 9;
    private static final int TELEPORT_DURATION = 10;
    private static final int TRANSLATION = 11;
    private static final int SCALE = 12;
    private static final int LEFT_ROTATION = 13;
    private static final int RIGHT_ROTATION = 14;
    private static final int BILLBOARD = 15;
    private static final int BRIGHTNESS = 16;
    private static final int VIEW_RANGE = 17;
    private static final int GLOW_COLOR = 22;
    private static final int ITEM = 23;
    private static final int NO_GLOW_COLOR = -1;
    private static final int NO_BRIGHTNESS = -1;
    private static final float VIEW_RANGE_BLOCKS = 64f;
    private static final Quaternion4f NO_ROTATION = new Quaternion4f(0f, 0f, 0f, 1f);

    private DisplayData() {
    }

    /**
     * Add everything a display needs when it spawns.
     *
     * @param into the list to add to
     * @param transform where the display is
     * @param look how it looks
     * @param item the item it shows
     * @param interpolation over how many ticks later moves are smoothed
     * @param viewDistance how far away, in blocks, the display should still be drawn
     */
    public static void spawn(
            final List<EntityData<?>> into,
            final DisplayTransform transform,
            final DisplayLook look,
            final ItemStack item,
            final int interpolation,
            final int viewDistance
    ) {
        into.add(new EntityData<>(TRANSFORMATION_DURATION, EntityDataTypes.INT, interpolation));
        into.add(new EntityData<>(TELEPORT_DURATION, EntityDataTypes.INT, interpolation));
        into.add(new EntityData<>(TRANSLATION, EntityDataTypes.VECTOR3F, vector(transform.translation())));
        into.add(new EntityData<>(SCALE, EntityDataTypes.VECTOR3F, vector(transform.scale())));
        into.add(new EntityData<>(LEFT_ROTATION, EntityDataTypes.QUATERNION, quaternion(transform.rotation())));
        into.add(new EntityData<>(RIGHT_ROTATION, EntityDataTypes.QUATERNION, NO_ROTATION));
        float range = Math.max(1f, viewDistance / VIEW_RANGE_BLOCKS);
        into.add(new EntityData<>(VIEW_RANGE, EntityDataTypes.FLOAT, range));
        look(into, look);
        item(into, item);
    }

    /**
     * Add a move, smoothed over the given ticks from the frame it arrives.
     *
     * @param into the list to add to
     * @param transform where the display is now
     * @param interpolation over how many ticks the move is smoothed
     */
    public static void transform(final List<EntityData<?>> into, final DisplayTransform transform, final int interpolation) {
        into.add(new EntityData<>(INTERPOLATION_DELAY, EntityDataTypes.INT, 0));
        into.add(new EntityData<>(TRANSFORMATION_DURATION, EntityDataTypes.INT, interpolation));
        into.add(new EntityData<>(TRANSLATION, EntityDataTypes.VECTOR3F, vector(transform.translation())));
        into.add(new EntityData<>(SCALE, EntityDataTypes.VECTOR3F, vector(transform.scale())));
        into.add(new EntityData<>(LEFT_ROTATION, EntityDataTypes.QUATERNION, quaternion(transform.rotation())));
    }

    /**
     * Add the outline, its color, the billboard and the light level.
     *
     * @param into the list to add to
     * @param look how the display looks now
     */
    public static void look(final List<EntityData<?>> into, final DisplayLook look) {
        into.add(new EntityData<>(FLAGS, EntityDataTypes.BYTE, look.glowing() ? GLOWING : (byte) 0));
        into.add(new EntityData<>(GLOW_COLOR, EntityDataTypes.INT, rgb(look.glowColor(), NO_GLOW_COLOR)));
        into.add(new EntityData<>(BILLBOARD, EntityDataTypes.BYTE, billboard(look.billboard())));
        into.add(new EntityData<>(BRIGHTNESS, EntityDataTypes.INT, brightness(look.brightness())));
    }

    /**
     * Add the item the display shows.
     *
     * @param into the list to add to
     * @param item the item, {@link ItemStack#EMPTY} to show nothing
     */
    public static void item(final List<EntityData<?>> into, final ItemStack item) {
        into.add(new EntityData<>(ITEM, EntityDataTypes.ITEMSTACK, item));
    }

    /**
     * Return the byte a display stores its billboard as.
     *
     * @param billboard the billboard
     * @return the byte
     */
    private static byte billboard(final Display.Billboard billboard) {
        return switch (billboard) {
            case FIXED -> 0;
            case VERTICAL -> 1;
            case HORIZONTAL -> 2;
            case CENTER -> 3;
        };
    }

    /**
     * Pack a light level the way displays store it.
     *
     * @param brightness the light level, or null for the world's light
     * @return the packed value, -1 for the world's light
     */
    static int brightness(final Display.@Nullable Brightness brightness) {
        if (brightness == null) {
            return NO_BRIGHTNESS;
        }

        return brightness.getBlockLight() << 4 | brightness.getSkyLight() << 20;
    }

    /**
     * Return a color as RGB, or a fallback for none.
     *
     * @param color the color, or null
     * @param fallback the value for null
     * @return the RGB value
     */
    private static int rgb(final @Nullable Color color, final int fallback) {
        return color == null ? fallback : color.asRGB();
    }

    /**
     * Convert a JOML vector to PacketEvents'.
     *
     * @param vector the vector
     * @return the copy
     */
    private static Vector3f vector(final Vector3fc vector) {
        return new Vector3f(vector.x(), vector.y(), vector.z());
    }

    /**
     * Convert a JOML quaternion to PacketEvents'.
     *
     * @param quaternion the quaternion
     * @return the copy
     */
    private static Quaternion4f quaternion(final Quaternionfc quaternion) {
        return new Quaternion4f(quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
    }
}
