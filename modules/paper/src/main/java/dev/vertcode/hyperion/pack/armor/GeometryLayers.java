package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector2fc;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.armor.ArmorBone;
import dev.vertcode.hyperion.armor.ArmorGeometry;
import dev.vertcode.hyperion.armor.ArmorLayer;
import dev.vertcode.hyperion.armor.ArmorPart;
import dev.vertcode.hyperion.armor.ArmorQuad;
import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.blueprint.data.Group;

/**
 * Encode a set's 3D faces on one vanilla armor texture into the layers the shader draws.
 *
 * <p>Vanilla draws each equipment layer on the same boxes, so every layer is another chance to
 * draw faces. The shader uses each box's front and back face, so one layer draws two faces per
 * limb, and a limb with twelve faces needs six layers. Each layer's image holds the records of
 * its faces and a copy of the model's textures.
 *
 * @param images the layers' images, empty when the set draws nothing in 3D here
 * @param parts the mask of limbs that have 3D faces
 * @param dropped how many faces did not fit in the layers allowed
 */
record GeometryLayers(List<BufferedImage> images, int parts, int dropped) {

    private static final int SIDES = 2;

    /**
     * Copy the images list, so the result never changes.
     */
    GeometryLayers {
        images = List.copyOf(images);
    }

    /**
     * Encode a set's faces on one texture.
     *
     * @param set the set
     * @param layer the texture
     * @param maxLayers the most layers allowed
     * @return the layers
     */
    static GeometryLayers encode(final HyperionArmorSet set, final ArmorLayer layer, final int maxLayers) {
        Map<ArmorPart, List<ArmorQuad>> quads = quads(set, layer);
        int most = 0;
        int mask = 0;
        for (Map.Entry<ArmorPart, List<ArmorQuad>> entry : quads.entrySet()) {
            most = Math.max(most, entry.getValue().size());
            if (!entry.getValue().isEmpty()) {
                mask |= entry.getKey().bit();
            }
        }

        if (most == 0) {
            return new GeometryLayers(List.of(), 0, 0);
        }

        int count = Math.min(Math.max(1, maxLayers), (most + SIDES - 1) / SIDES);
        int dropped = 0;
        for (List<ArmorQuad> list : quads.values()) {
            dropped += Math.max(0, list.size() - count * SIDES);
        }

        ArmorAtlas atlas = new ArmorAtlas(set);
        BufferedImage textures = atlas.image();
        int width = ArmorShaderData.ATLAS_X + textures.getWidth();
        int height = Math.max(ArmorShaderData.RECORD_ROWS, textures.getHeight());
        List<BufferedImage> images = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            BufferedImage image = ArmorImages.blank(width, height);
            ArmorShaderData.header(image, ArmorShaderData.KIND_GEOMETRY, mask, layer.inflate());
            // Copied pixel by pixel: drawing would blend, and lose the colors of near transparent pixels.
            for (int y = 0; y < textures.getHeight(); y++) {
                for (int x = 0; x < textures.getWidth(); x++) {
                    image.setRGB(ArmorShaderData.ATLAS_X + x, y, textures.getRGB(x, y));
                }
            }

            for (Map.Entry<ArmorPart, List<ArmorQuad>> entry : quads.entrySet()) {
                for (int side = 0; side < SIDES; side++) {
                    int quad = index * SIDES + side;
                    if (quad < entry.getValue().size()) {
                        write(image, row(entry.getKey(), side), entry.getValue().get(quad), atlas);
                    }
                }
            }

            images.add(image);
        }

        return new GeometryLayers(images, mask, dropped);
    }

    /**
     * Return the row a limb's face is stored in.
     *
     * @param part the limb
     * @param side 0 for the front face, 1 for the back
     * @return the row
     */
    static int row(final ArmorPart part, final int side) {
        return 1 + part.index() * SIDES + side;
    }

    /**
     * Collect the faces of every bone on a texture, by the limb they follow.
     *
     * @param set the set
     * @param layer the texture
     * @return the faces by limb
     */
    static Map<ArmorPart, List<ArmorQuad>> quads(final HyperionArmorSet set, final ArmorLayer layer) {
        Map<ArmorPart, List<ArmorQuad>> quads = new EnumMap<>(ArmorPart.class);
        for (Map.Entry<ArmorBone, Group> entry : set.bones().entrySet()) {
            ArmorBone bone = entry.getKey();
            ArmorPart part = bone.part();
            if (part == null || bone.layer() != layer) {
                continue;
            }

            quads.computeIfAbsent(part, key -> new ArrayList<>())
                    .addAll(ArmorGeometry.quads(set.model(), entry.getValue(), part.pivot()));
        }

        return quads;
    }

    /**
     * Write one face's record.
     *
     * @param image the layer's image
     * @param row the record's row
     * @param quad the face
     * @param atlas where the face's texture sits in the image
     */
    private static void write(final BufferedImage image, final int row, final ArmorQuad quad, final ArmorAtlas atlas) {
        for (int corner = 0; corner < 4; corner++) {
            Vector3fc position = quad.corners().get(corner);
            Vector2fc uv = atlas.pixel(quad.texture(), quad.uvs().get(corner));
            int x = corner * ArmorShaderData.CORNER_PIXELS;
            image.setRGB(x, row, ArmorShaderData.pixel(ArmorShaderData.position(position.x())));
            image.setRGB(x + 1, row, ArmorShaderData.pixel(ArmorShaderData.position(position.y())));
            image.setRGB(x + 2, row, ArmorShaderData.pixel(ArmorShaderData.position(position.z())));
            image.setRGB(x + 3, row, ArmorShaderData.pixel(ArmorShaderData.uv(ArmorShaderData.ATLAS_X + uv.x())));
            image.setRGB(x + 4, row, ArmorShaderData.pixel(ArmorShaderData.uv(uv.y())));
        }

        Vector3fc normal = quad.normal();
        image.setRGB(ArmorShaderData.RECORD_PIXELS - 1, row, ArmorImages.argb(
                normalByte(normal.x()),
                normalByte(normal.y()),
                normalByte(normal.z()),
                ArmorShaderData.DATA_ALPHA
        ));
    }

    /**
     * Store one part of a unit normal in a byte, -1 to 1 as 1 to 255.
     *
     * @param value the part
     * @return the byte
     */
    static int normalByte(final float value) {
        return Math.round(Math.max(-1f, Math.min(1f, value)) * 127f) + 128;
    }
}
