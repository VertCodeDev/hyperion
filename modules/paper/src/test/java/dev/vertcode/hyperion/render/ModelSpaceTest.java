package dev.vertcode.hyperion.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.animation.BonePose;
import dev.vertcode.hyperion.api.model.ModelRotation;
import dev.vertcode.hyperion.api.model.bone.BoneTransform;
import dev.vertcode.hyperion.pack.ModelPart;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelSpaceTest {

    private static final float EPSILON = 1.0e-5f;

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
        Matrix4f bone = bone(new Matrix4f(), pivot, new Vector3f(0, 0, 90), new BonePose(), BoneTransform.IDENTITY);

        assertVector(pivot, bone.transformPosition(new Vector3f(pivot)));
        assertVector(new Vector3f(0, 2, 0), bone.transformPosition(new Vector3f(1, 1, 0)));
    }

    @Test
    void movesChildrenWithTheirParent() {
        Matrix4f parent = bone(new Matrix4f(), new Vector3f(), new Vector3f(), new BonePose(), BoneTransform.ofTranslation(0, 1, 0));
        Matrix4f child = bone(parent, new Vector3f(0, 2, 0), new Vector3f(), new BonePose(), BoneTransform.IDENTITY);

        assertVector(new Vector3f(0, 3, 0), child.transformPosition(new Vector3f(0, 2, 0)));
    }

    @Test
    void addsAnAnimationsAnglesToTheRestAngles() {
        BonePose pose = new BonePose();
        pose.rotation().set(0, 0, 45);
        Matrix4f animated = bone(new Matrix4f(), new Vector3f(), new Vector3f(0, 0, 45), pose, BoneTransform.IDENTITY);
        Matrix4f quarter = bone(new Matrix4f(), new Vector3f(), new Vector3f(0, 0, 90), new BonePose(), BoneTransform.IDENTITY);

        assertVector(quarter.transformPosition(new Vector3f(1, 0, 0)), animated.transformPosition(new Vector3f(1, 0, 0)));
    }

    @Test
    void movesAndStretchesABoneByItsPose() {
        BonePose pose = new BonePose();
        pose.position().set(0, 1, 0);
        pose.scale().set(2f);
        Matrix4f bone = bone(new Matrix4f(), new Vector3f(), new Vector3f(), pose, BoneTransform.IDENTITY);

        assertVector(new Vector3f(2, 1, 0), bone.transformPosition(new Vector3f(1, 0, 0)));
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

    private static Matrix4f bone(
            final Matrix4f parent,
            final Vector3f pivot,
            final Vector3f rest,
            final BonePose pose,
            final BoneTransform transform
    ) {
        return ModelSpace.bone(new Matrix4f(), parent, pivot, rest, pose, transform, new Quaternionf());
    }

    private static void assertVector(final Vector3f expected, final Vector3f actual) {
        assertTrue(expected.equals(actual, EPSILON), "expected " + expected + " but was " + actual);
    }
}
