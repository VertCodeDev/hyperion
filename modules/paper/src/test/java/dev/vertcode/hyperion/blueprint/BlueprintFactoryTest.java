package dev.vertcode.hyperion.blueprint;

import java.io.IOException;
import java.util.List;

import org.bukkit.util.BoundingBox;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.locator.LocatorBlueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintFactoryTest {

    private static final float EPSILON = 1.0e-5f;

    @Test
    void turnsXFirstThenYThenZ() {
        // Blockbench turns in ZYX order: X first. X then Y sends +X to -Z; Y then X would send it to +Y.
        Vector3f turned = BlockbenchRotation.of(new Vector3f(90, 90, 0)).transform(new Vector3f(1, 0, 0));

        assertVector(new Vector3f(0, 0, -1), turned);
    }

    @Test
    void buildsTheBoneTreeInBlocks() throws IOException {
        LoadReport report = new LoadReport("groups_layout.bbmodel");
        Blueprint blueprint = Fixtures.blueprint("groups_layout", report);

        assertEquals(List.of("body", "head"), blueprint.bones().stream().map(BoneBlueprint::name).toList());
        assertEquals(List.of("body"), blueprint.rootBones().stream().map(BoneBlueprint::name).toList());

        BoneBlueprint head = blueprint.bone("head").orElseThrow();
        assertEquals("body", head.parent().orElseThrow().name());
        assertVector(new Vector3f(0, 0.5f, 0), new Vector3f(head.pivot()));
        assertEquals(new Quaternionf().rotationZYX(0, (float) Math.toRadians(90), (float) Math.toRadians(90)), head.rotation());
        assertFalse(head.visibleByDefault());
        assertTrue(head.hasGeometry());
        assertEquals(List.of("head"), blueprint.bone("body").orElseThrow().children().stream().map(BoneBlueprint::name).toList());
    }

    @Test
    void placesLocatorsRelativeToTheirBone() throws IOException {
        Blueprint blueprint = Fixtures.blueprint("groups_layout", new LoadReport("groups_layout.bbmodel"));
        LocatorBlueprint mouth = blueprint.locator("mouth").orElseThrow();

        assertEquals("head", mouth.parent().orElseThrow().name());
        assertVector(new Vector3f(0, 0.125f, -0.25f), new Vector3f(mouth.position()));
    }

    @Test
    void renamesRepeatedAnimationNames() throws IOException {
        LoadReport report = new LoadReport("groups_layout.bbmodel");
        Blueprint blueprint = Fixtures.blueprint("groups_layout", report);

        assertEquals(List.of("walk", "idle", "idle_2"), List.copyOf(blueprint.animations()));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.describe().equals(
                "animation 'idle': the name is already taken, so it is renamed to 'idle_2'"
        )));
    }

    @Test
    void renamesRepeatedBonesAndKeepsLooseCubes() throws IOException {
        LoadReport report = new LoadReport("legacy_layout.bbmodel");
        Blueprint blueprint = Fixtures.blueprint("legacy_layout", report);

        assertEquals(List.of("root", "arm", "arm_2", "root_2"), blueprint.bones().stream().map(BoneBlueprint::name).toList());
        assertEquals("pebble", blueprint.blueprintBones().getLast().cubes().getFirst().name());
        assertTrue(report.issues().stream().anyMatch(issue -> issue.message().contains("sit outside every group")));

        LocatorBlueprint tip = blueprint.locator("tip").orElseThrow();
        assertEquals("arm", tip.parent().orElseThrow().name());
        assertVector(new Vector3f(1 / 16f, -6 / 16f, 0), new Vector3f(tip.position()));
    }

    @Test
    void measuresTheBoundingBoxWithRestRotations() throws IOException {
        BoundingBox box = Fixtures.blueprint("legacy_layout", new LoadReport("legacy_layout.bbmodel")).boundingBox();

        assertEquals(-0.375, box.getMinX(), EPSILON);
        assertEquals(0.0, box.getMinY(), EPSILON);
        assertEquals(-0.0625, box.getMinZ(), EPSILON);
        assertEquals(0.6875, box.getMaxX(), EPSILON);
        assertEquals(0.8383883, box.getMaxY(), EPSILON);
        assertEquals(0.6875, box.getMaxZ(), EPSILON);
    }

    @Test
    void centersJavaBlockModelsOnTheOrigin() throws IOException {
        Blueprint blueprint = Fixtures.blueprint("java_block", new LoadReport("java_block.bbmodel"));
        BoundingBox box = blueprint.boundingBox();

        assertEquals(-0.5, box.getMinX(), EPSILON);
        assertEquals(0.5, box.getMaxX(), EPSILON);
        assertEquals(1.0, box.getMaxY(), EPSILON);
        assertVector(new Vector3f(0, 0.5f, 0), new Vector3f(blueprint.bone("block").orElseThrow().pivot()));
        assertEquals(new Vector3f(-8, 0, -8), blueprint.blueprintBones().getFirst().cubes().getFirst().from());
    }

    private static void assertVector(final Vector3f expected, final Vector3f actual) {
        assertTrue(expected.equals(actual, EPSILON), "expected " + expected + " but was " + actual);
    }
}
