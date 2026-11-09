package dev.vertcode.hyperion.render;

import java.util.List;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The entity data of the text, item and block displays effects show as props.
 *
 * <p>The indexes are the ones every version from 1.20.2 on uses. Each method adds the entries of
 * one kind of change, so a prop sends only what changed.
 */
public final class PropData {

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
    private static final int CONTENT = 23;
    private static final int ITEM_DISPLAY = 24;
    private static final int LINE_WIDTH = 24;
    private static final int BACKGROUND = 25;
    private static final int TEXT_OPACITY = 26;
    private static final int TEXT_FLAGS = 27;
    private static final byte SHADOW = 0x01;
    private static final byte SEE_THROUGH = 0x02;
    private static final byte ALIGN_LEFT = 0x08;
    private static final byte ALIGN_RIGHT = 0x10;
    private static final Quaternion4f NO_ROTATION = new Quaternion4f(0f, 0f, 0f, 1f);

    private PropData() {
    }

    /**
     * Add what every prop needs when it spawns: how its moves are smoothed, its billboard, light
     * and view range, and its outline.
     *
     * @param into the list to add to
     * @param smoothing over how many ticks moves are smoothed
     * @param billboard how it faces the viewer
     * @param brightness its light, or null for the world's
     * @param viewRange how far away it is drawn, as a multiple of 64 blocks
     * @param glowColor the color of its outline, or null for none
     */
    public static void base(
            final List<EntityData<?>> into,
            final int smoothing,
            final Display.Billboard billboard,
            final Display.@Nullable Brightness brightness,
            final float viewRange,
            final @Nullable Color glowColor
    ) {
        into.add(new EntityData<>(TELEPORT_DURATION, EntityDataTypes.INT, smoothing));
        into.add(new EntityData<>(BILLBOARD, EntityDataTypes.BYTE, billboard(billboard)));
        into.add(new EntityData<>(BRIGHTNESS, EntityDataTypes.INT, DisplayData.brightness(brightness)));
        into.add(new EntityData<>(VIEW_RANGE, EntityDataTypes.FLOAT, viewRange));
        into.add(new EntityData<>(DisplayData.FLAGS, EntityDataTypes.BYTE, glowColor == null ? (byte) 0 : DisplayData.GLOWING));
        into.add(new EntityData<>(GLOW_COLOR, EntityDataTypes.INT, glowColor == null ? -1 : glowColor.asRGB()));
    }

    /**
     * Add a prop's offset from its spot, rotation and scale, smoothed over some ticks.
     *
     * @param into the list to add to
     * @param translation the offset from the prop's spot, in the world
     * @param rotation the rotation
     * @param scale the scale on each axis
     * @param duration over how many ticks the client smooths the change
     */
    public static void transform(
            final List<EntityData<?>> into,
            final Vector3fc translation,
            final Quaternionfc rotation,
            final Vector3fc scale,
            final int duration
    ) {
        into.add(new EntityData<>(INTERPOLATION_DELAY, EntityDataTypes.INT, 0));
        into.add(new EntityData<>(TRANSFORMATION_DURATION, EntityDataTypes.INT, duration));
        into.add(new EntityData<>(TRANSLATION, EntityDataTypes.VECTOR3F, new Vector3f(translation.x(), translation.y(), translation.z())));
        into.add(new EntityData<>(SCALE, EntityDataTypes.VECTOR3F, new Vector3f(scale.x(), scale.y(), scale.z())));
        into.add(new EntityData<>(
                LEFT_ROTATION,
                EntityDataTypes.QUATERNION,
                new Quaternion4f(rotation.x(), rotation.y(), rotation.z(), rotation.w())
        ));
        into.add(new EntityData<>(RIGHT_ROTATION, EntityDataTypes.QUATERNION, NO_ROTATION));
    }

    /**
     * Add the item an item display shows, and how.
     *
     * @param into the list to add to
     * @param item the item
     * @param display how it is held, as the item display mode's number
     */
    public static void item(final List<EntityData<?>> into, final ItemStack item, final byte display) {
        into.add(new EntityData<>(CONTENT, EntityDataTypes.ITEMSTACK, item));
        into.add(new EntityData<>(ITEM_DISPLAY, EntityDataTypes.BYTE, display));
    }

    /**
     * Add the block a block display shows.
     *
     * @param into the list to add to
     * @param blockState the block state's global id
     */
    public static void block(final List<EntityData<?>> into, final int blockState) {
        into.add(new EntityData<>(CONTENT, EntityDataTypes.BLOCK_STATE, blockState));
    }

    /**
     * Add a text display's text and style.
     *
     * @param into the list to add to
     * @param text the text
     * @param style how it is drawn
     */
    public static void text(final List<EntityData<?>> into, final Component text, final TextStyle style) {
        into.add(new EntityData<>(CONTENT, EntityDataTypes.ADV_COMPONENT, text));
        into.add(new EntityData<>(LINE_WIDTH, EntityDataTypes.INT, style.lineWidth()));
        into.add(new EntityData<>(BACKGROUND, EntityDataTypes.INT, style.background()));
        into.add(new EntityData<>(TEXT_OPACITY, EntityDataTypes.BYTE, style.opacity()));
        byte flags = 0;
        if (style.shadow()) {
            flags |= SHADOW;
        }

        if (style.seeThrough()) {
            flags |= SEE_THROUGH;
        }

        if (style.alignment() < 0) {
            flags |= ALIGN_LEFT;
        } else if (style.alignment() > 0) {
            flags |= ALIGN_RIGHT;
        }

        into.add(new EntityData<>(TEXT_FLAGS, EntityDataTypes.BYTE, flags));
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
     * How a text prop is drawn.
     *
     * @param lineWidth the width a line wraps at, in pixels
     * @param background the background as ARGB
     * @param opacity the text's opacity, -1 for full
     * @param shadow whether the text has a shadow
     * @param seeThrough whether it shows through blocks
     * @param alignment -1 left, 0 centered, 1 right
     */
    public record TextStyle(int lineWidth, int background, byte opacity, boolean shadow, boolean seeThrough, int alignment) {
    }
}
