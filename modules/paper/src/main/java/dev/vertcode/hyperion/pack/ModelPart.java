package dev.vertcode.hyperion.pack;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.skin.SkinMap;

/**
 * One item model a bone is drawn with, and how to place it relative to the bone.
 *
 * <p>Most bones are one part. A cube turned at an angle an item model cannot hold gets a part of
 * its own, turned by the display instead; a bone too big for an item model is shrunk in the
 * pack and scaled back up by the display. Rendering applies, on top of the bone's transform:
 * move by {@link #offset()}, turn by {@link #rotation()}, scale by {@link #scale()}.
 *
 * @param model the item model's id, such as {@code hyperion:bosses/dragon/jaw}, which is also the
 *     {@code item_model} value from 1.21.4
 * @param customModelData the number that picks this model on 1.21 to 1.21.3
 * @param offset where the part's origin sits from the bone's pivot, in blocks
 * @param rotation how the part is turned
 * @param scale how much the display scales the part up, 1 when it was not shrunk
 * @param skin the skin pixels the part shows, or null when it shows none
 */
public record ModelPart(
        String model,
        int customModelData,
        Vector3fc offset,
        Quaternionfc rotation,
        float scale,
        @Nullable SkinMap skin
) {

    /**
     * Copy the offset and rotation, so a part never changes after it is built.
     */
    public ModelPart {
        offset = new Vector3f(offset);
        rotation = new Quaternionf(rotation);
    }

    /**
     * Describe a part that shows no skin.
     *
     * @param model the item model's id
     * @param customModelData the number that picks this model on 1.21 to 1.21.3
     * @param offset where the part's origin sits from the bone's pivot, in blocks
     * @param rotation how the part is turned
     * @param scale how much the display scales the part up
     */
    public ModelPart(
            final String model,
            final int customModelData,
            final Vector3fc offset,
            final Quaternionfc rotation,
            final float scale
    ) {
        this(model, customModelData, offset, rotation, scale, null);
    }
}
