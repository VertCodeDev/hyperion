package dev.vertcode.hyperion.armor;

import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorGeometryTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void placesACubeOnTheTemplateExactlyOnVanillasBox() {
        HyperionArmorSet set = ArmorFixtures.frostSet(MinecraftVersion.parse("1.21.4"));
        List<ArmorQuad> quads = ArmorGeometry.quads(set.model(), set.bones().get(ArmorBone.RIGHT_ARM), ArmorPart.RIGHT_ARM.pivot());
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        for (ArmorQuad quad : quads) {
            for (Vector3fc corner : quad.corners()) {
                min.min(corner);
                max.max(corner);
            }
        }

        Vector3fc vanillaMin = ArmorPart.RIGHT_ARM.min();
        Vector3f vanillaMax = new Vector3f(vanillaMin).add(ArmorPart.RIGHT_ARM.size());
        assertEquals(6, quads.size());
        assertTrue(min.equals(vanillaMin, EPSILON), min.toString());
        assertTrue(max.equals(vanillaMax, EPSILON), max.toString());
    }

    @Test
    void flipsXAndYAroundThePivot() {
        Vector3f limb = ArmorGeometry.toLimb(new Vector3f(6, 20, 1), new Vector3f(5, 22, 0));

        assertTrue(limb.equals(new Vector3f(-1, 2, 1), EPSILON));
    }

    @Test
    void pointsNormalsTheWayTheLimbsSpaceDoes() {
        HyperionArmorSet set = ArmorFixtures.frostSet(MinecraftVersion.parse("1.21.4"));
        List<ArmorQuad> quads = ArmorGeometry.quads(set.model(), set.bones().get(ArmorBone.RIGHT_ARM), ArmorPart.RIGHT_ARM.pivot());

        // Faces come in Blockbench's order: north, east, south, west, up, down.
        assertTrue(new Vector3f(0, 0, -1).equals(quads.get(0).normal(), EPSILON));
        assertTrue(new Vector3f(-1, 0, 0).equals(quads.get(1).normal(), EPSILON));
        assertTrue(new Vector3f(0, -1, 0).equals(quads.get(4).normal(), EPSILON));
    }

    @Test
    void listsAFacesCornersClockwiseFromTheTopLeftSeenFromOutside() {
        Vector3f[] north = ArmorGeometry.corners(FaceSide.NORTH, new Vector3f(0, 0, 0), new Vector3f(2, 3, 4));

        assertTrue(north[0].equals(new Vector3f(2, 3, 0), EPSILON));
        assertTrue(north[1].equals(new Vector3f(0, 3, 0), EPSILON));
        assertTrue(north[2].equals(new Vector3f(0, 0, 0), EPSILON));
        assertTrue(north[3].equals(new Vector3f(2, 0, 0), EPSILON));
    }

    @Test
    void turnsAFacesUvWithItsRotation() {
        List<Vector2fc> plain = ArmorGeometry.uvs(new CubeFace(0, 0, 4, 2, 0, 0));
        List<Vector2fc> turned = ArmorGeometry.uvs(new CubeFace(0, 0, 4, 2, 0, 90));

        assertEquals(new Vector2f(0, 0), plain.get(0));
        assertEquals(new Vector2f(4, 2), plain.get(2));
        assertEquals(new Vector2f(0, 2), turned.get(0));
        assertEquals(new Vector2f(0, 0), turned.get(1));
    }
}
