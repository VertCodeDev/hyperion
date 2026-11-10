package dev.vertcode.hyperion.armor;

import java.util.Locale;
import java.util.Optional;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorSlot;

/**
 * A bone an armor set's {@code .bbmodel} can have, and the limb it follows.
 *
 * <p>The helmet is worn as an item, so it follows the head with no texture layer. Every other
 * bone is drawn on one of vanilla's armor boxes, on the outer or inner texture.
 */
public enum ArmorBone {

    /**
     * The helmet, on the head.
     */
    HELMET("helmet", ArmorSlot.HEAD, null, null),

    /**
     * The chestplate's body.
     */
    CHESTPLATE("chestplate", ArmorSlot.CHEST, ArmorLayer.OUTER, ArmorPart.BODY),

    /**
     * The chestplate's right arm.
     */
    RIGHT_ARM("right_arm", ArmorSlot.CHEST, ArmorLayer.OUTER, ArmorPart.RIGHT_ARM),

    /**
     * The chestplate's left arm.
     */
    LEFT_ARM("left_arm", ArmorSlot.CHEST, ArmorLayer.OUTER, ArmorPart.LEFT_ARM),

    /**
     * The leggings' waist.
     */
    LEGGINGS("leggings", ArmorSlot.LEGS, ArmorLayer.INNER, ArmorPart.BODY),

    /**
     * The leggings' right leg.
     */
    RIGHT_LEG("right_leg", ArmorSlot.LEGS, ArmorLayer.INNER, ArmorPart.RIGHT_LEG),

    /**
     * The leggings' left leg.
     */
    LEFT_LEG("left_leg", ArmorSlot.LEGS, ArmorLayer.INNER, ArmorPart.LEFT_LEG),

    /**
     * The right boot.
     */
    RIGHT_BOOT("right_boot", ArmorSlot.FEET, ArmorLayer.OUTER, ArmorPart.RIGHT_LEG),

    /**
     * The left boot.
     */
    LEFT_BOOT("left_boot", ArmorSlot.FEET, ArmorLayer.OUTER, ArmorPart.LEFT_LEG);

    // The neck, where the head turns, in Blockbench's space.
    private static final float HEAD_PIVOT_Y = 24f;

    private final String bone;
    private final ArmorSlot slot;
    private final @Nullable ArmorLayer layer;
    private final @Nullable ArmorPart part;

    ArmorBone(
            final String bone,
            final ArmorSlot slot,
            final @Nullable ArmorLayer layer,
            final @Nullable ArmorPart part
    ) {
        this.bone = bone;
        this.slot = slot;
        this.layer = layer;
        this.part = part;
    }

    /**
     * Return the bone's name in a {@code .bbmodel}.
     *
     * @return the name
     */
    public String bone() {
        return this.bone;
    }

    /**
     * Return the piece this bone belongs to.
     *
     * @return the slot
     */
    public ArmorSlot slot() {
        return this.slot;
    }

    /**
     * Return the texture this bone is drawn on.
     *
     * @return the layer, or null for the helmet, which is worn as an item
     */
    public @Nullable ArmorLayer layer() {
        return this.layer;
    }

    /**
     * Return the box this bone is drawn on.
     *
     * @return the part, or null for the helmet
     */
    public @Nullable ArmorPart part() {
        return this.part;
    }

    /**
     * Return the limb's pivot in Blockbench's space, where this bone should pivot.
     *
     * @return the pivot, in pixels
     */
    public Vector3fc pivot() {
        return this.part == null ? new Vector3f(0, HEAD_PIVOT_Y, 0) : this.part.pivot();
    }

    /**
     * Find a bone by its name in a {@code .bbmodel}.
     *
     * @param name the name, in any case
     * @return the bone, or empty when the name is no armor bone
     */
    public static Optional<ArmorBone> fromName(final String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        for (ArmorBone bone : values()) {
            if (bone.bone.equals(lower)) {
                return Optional.of(bone);
            }
        }

        return Optional.empty();
    }
}
