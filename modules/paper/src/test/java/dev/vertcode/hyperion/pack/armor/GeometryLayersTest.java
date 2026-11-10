package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.armor.ArmorFixtures;
import dev.vertcode.hyperion.armor.ArmorLayer;
import dev.vertcode.hyperion.armor.ArmorPart;
import dev.vertcode.hyperion.armor.ArmorQuad;
import dev.vertcode.hyperion.armor.HyperionArmorSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryLayersTest {

    private static final HyperionArmorSet FROST = ArmorFixtures.frostSet(MinecraftVersion.parse("26.3"));
    private static final float EPSILON = 1f / ArmorShaderData.POSITION_SCALE;

    @Test
    void usesALayerForEveryTwoFacesOfTheBusiestLimb() {
        GeometryLayers layers = GeometryLayers.encode(FROST, ArmorLayer.OUTER, 16);

        // The body has two cubes, twelve faces.
        assertEquals(6, layers.images().size());
        assertEquals(ArmorPart.BODY.bit() | ArmorPart.RIGHT_ARM.bit() | ArmorPart.RIGHT_LEG.bit(), layers.parts());
        assertEquals(0, layers.dropped());
    }

    @Test
    void countsTheFacesThatDoNotFit() {
        GeometryLayers layers = GeometryLayers.encode(FROST, ArmorLayer.OUTER, 2);

        assertEquals(2, layers.images().size());
        assertEquals(12 - 4 + 6 - 4 + 6 - 4, layers.dropped());
    }

    @Test
    void writesTheMagicAndHeader() {
        BufferedImage image = GeometryLayers.encode(FROST, ArmorLayer.OUTER, 16).images().get(0);

        assertEquals(ArmorImages.argb(72, 89, 80, 3), image.getRGB(0, 0));
        assertEquals(ArmorShaderData.KIND_GEOMETRY, image.getRGB(1, 0) >> 8 & 0xFF);
        assertEquals(64, image.getRGB(2, 0) & 0xFF);
    }

    @Test
    void storesEachFacesCornersSoTheyReadBack() {
        GeometryLayers layers = GeometryLayers.encode(FROST, ArmorLayer.OUTER, 16);
        List<ArmorQuad> body = GeometryLayers.quads(FROST, ArmorLayer.OUTER).get(ArmorPart.BODY);
        BufferedImage second = layers.images().get(1);
        int row = GeometryLayers.row(ArmorPart.BODY, 1);
        ArmorQuad quad = body.get(3);
        for (int corner = 0; corner < 4; corner++) {
            Vector3fc expected = quad.corners().get(corner);
            int x = corner * ArmorShaderData.CORNER_PIXELS;

            assertEquals(expected.x(), position(second.getRGB(x, row)), EPSILON);
            assertEquals(expected.y(), position(second.getRGB(x + 1, row)), EPSILON);
            assertEquals(expected.z(), position(second.getRGB(x + 2, row)), EPSILON);
        }

        int u = ArmorShaderData.value(second.getRGB(3, row));
        assertTrue(u >= ArmorShaderData.ATLAS_X * ArmorShaderData.UV_SCALE);
    }

    @Test
    void leavesTheRowsOfLimbsWithoutFacesEmpty() {
        BufferedImage image = GeometryLayers.encode(FROST, ArmorLayer.OUTER, 16).images().get(0);

        assertEquals(0, image.getRGB(0, GeometryLayers.row(ArmorPart.LEFT_ARM, 0)) >>> 24);
    }

    @Test
    void codesEveryModelPixelInvisibleToVanilla() {
        BufferedImage image = GeometryLayers.encode(FROST, ArmorLayer.OUTER, 16).images().get(0);

        // The plain texture comes first, the glowing one under it.
        assertEquals(ArmorShaderData.PLAIN, image.getRGB(ArmorShaderData.ATLAS_X, 0) >>> 24);
        assertEquals(ArmorShaderData.PLAIN + ArmorShaderData.GLOW, image.getRGB(ArmorShaderData.ATLAS_X, 4) >>> 24);
        assertEquals(0x3366CC, image.getRGB(ArmorShaderData.ATLAS_X, 0) & 0xFFFFFF);
    }

    @Test
    void drawsNothingOnATextureTheSetHasNoBonesFor() {
        GeometryLayers layers = GeometryLayers.encode(FROST, ArmorLayer.INNER, 16);
        Map<ArmorPart, List<ArmorQuad>> quads = GeometryLayers.quads(FROST, ArmorLayer.INNER);

        assertTrue(layers.images().isEmpty());
        assertTrue(quads.isEmpty());
    }

    /**
     * Read a stored position back.
     *
     * @param pixel the record pixel
     * @return the position, in pixels
     */
    private static float position(final int pixel) {
        return (ArmorShaderData.value(pixel) - ArmorShaderData.MIDDLE) / (float) ArmorShaderData.POSITION_SCALE;
    }
}
