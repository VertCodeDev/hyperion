package dev.vertcode.hyperion.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.ModelRotation;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.pack.ModelPart;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelSpaceTest {

    private static final float EPSILON = 1.0e-5f;
    private static final float QUARTER = (float) Math.toRadians(90);

    @Test
    void turnsANorthFacingModelToFaceWhereItsAnchorLooks() {
        Vector3f north = new Vector3f(0, 0, -1);

        Matrix4f south = ModelSpace.root(new Matrix4f(), ModelRotation.ZERO, 1f, BoneTransform.IDENTITY);
        assertVector(new Vector3f(0, 0, 1), south.transformPosition(new Vector3f(north)));

        Matrix4f west = ModelSpace.root(new Matrix4f(), new ModelRotation(0f, 90f), 1f, BoneTransform.IDENTITY);
        assertVector(new Vector3f(-1, 0, 0), west.transformPosition(new Vector3f(north)));
    }

    @Test
    void scalesAndOffsetsTheWholeModel() {
        BoneTransform raised = BoneTransform.ofTranslation(0, 1, 0);
        Matrix4f root = ModelSpace.root(new Matrix4f(), ModelRotation.ZERO, 2f, raised);

        assertVector(new Vector3f(0, 2, 0), root.transformPosition(new Vector3f()));
        assertVector(new Vector3f(0, 4, 0), root.transformPosition(new Vector3f(0, 1, 0)));
    }

    @Test
    void turnsABoneAroundItsPivot() {
        Vector3f pivot = new Vector3f(0, 1, 0);
        Quaternionf rest = new Quaternionf().rotationZ(QUARTER);
        Matrix4f bone = ModelSpace.bone(new Matrix4f(), new Matrix4f(), pivot, rest, BoneTransform.IDENTITY);

        assertVector(pivot, bone.transformPosition(new Vector3f(pivot)));
        assertVector(new Vector3f(0, 2, 0), bone.transformPosition(new Vector3f(1, 1, 0)));
    }

    @Test
    void movesChildrenWithTheirParent() {
        Matrix4f parent = ModelSpace.bone(
                new Matrix4f(),
                new Matrix4f(),
                new Vector3f(),
                new Quaternionf(),
                BoneTransform.ofTranslation(0, 1, 0)
        );
        Matrix4f child = ModelSpace.bone(new Matrix4f(), parent, new Vector3f(0, 2, 0), new Quaternionf(), BoneTransform.IDENTITY);

        assertVector(new Vector3f(0, 3, 0), child.transformPosition(new Vector3f(0, 2, 0)));
    }

    @Test
    void placesAPartAtItsBonesPivotAndUndoesTheDisplaysHalfTurn() {
        Matrix4f root = ModelSpace.root(new Matrix4f(), ModelRotation.ZERO, 1f, BoneTransform.IDENTITY);
        Vector3f pivot = new Vector3f(1, 2, 3);
        ModelPart part = new ModelPart("hyperion:test/part", 0, new Vector3f(), new Quaternionf(), 1f);

        DisplayTransform transform = DisplayTransform.of(ModelSpace.part(new Matrix4f(), root, new Matrix4f(), pivot, part));

        assertVector(new Vector3f(-1, 2, -3), new Vector3f(transform.translation()));
        assertTrue(transform.closeTo(new DisplayTransform(new Vector3f(-1, 2, -3), new Quaternionf(), new Vector3f(1)), EPSILON));
    }

    @Test
    void scalesAShrunkPartBackUp() {
        Matrix4f root = ModelSpace.root(new Matrix4f(), ModelRotation.ZERO, 1.5f, BoneTransform.IDENTITY);
        ModelPart part = new ModelPart("hyperion:test/big", 0, new Vector3f(0, 0, 1), new Quaternionf(), 2f);

        DisplayTransform transform = DisplayTransform.of(ModelSpace.part(new Matrix4f(), root, new Matrix4f(), new Vector3f(), part));

        assertVector(new Vector3f(3), new Vector3f(transform.scale()));
        assertVector(new Vector3f(0, 0, -1.5f), new Vector3f(transform.translation()));
    }

    private static void assertVector(final Vector3f expected, final Vector3f actual) {
        assertTrue(expected.equals(actual, EPSILON), "expected " + expected + " but was " + actual);
    }
}
