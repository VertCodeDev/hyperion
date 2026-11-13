package dev.vertcode.hyperion.pack;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.skin.SkinPixel;

/**
 * One skin pixel of a cube's face, as the flat square that draws it.
 *
 * @param from the square's lowest corner, in the model's pixels
 * @param to the square's highest corner; it equals {@code from} on the axis the face looks along
 * @param side the way the face looks
 * @param pixel the skin pixel it shows
 * @param optional whether it is drawn only for some skins: a pixel of the outer layer, or a cube
 *     made for one arm shape
 */
record SkinQuad(Vector3fc from, Vector3fc to, FaceSide side, SkinPixel pixel, boolean optional) {

    /**
     * Copy the corners, so a quad never changes after it is built.
     */
    SkinQuad {
        from = new Vector3f(from);
        to = new Vector3f(to);
    }
}
