package dev.vertcode.hyperion.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayTransformTest {

    private static final float EPSILON = 1.0e-5f;

    @Test
    void splitsAMatrixIntoTranslationTurnAndStretch() {
        Quaternionf turn = new Quaternionf().rotationXYZ(0.3f, -1.2f, 0.7f);
        Matrix4f matrix = new Matrix4f().translate(1, 2, 3).rotate(turn).scale(2, 3, 4);

        DisplayTransform transform = DisplayTransform.of(matrix);

        assertTrue(transform.closeTo(new DisplayTransform(new Vector3f(1, 2, 3), turn, new Vector3f(2, 3, 4)), EPSILON));
    }

    @Test
    void keepsAMirroredScale() {
        Matrix4f matrix = new Matrix4f().scale(-1f, -2f, -3f);

        DisplayTransform transform = DisplayTransform.of(matrix);

        assertTrue(new Vector3f(-1, -2, -3).equals(transform.scale(), EPSILON));
    }

    @Test
    void treatsBothSignsOfAQuaternionAsTheSameTurn() {
        Quaternionf turn = new Quaternionf().rotationY(1f);
        Quaternionf negated = new Quaternionf(-turn.x, -turn.y, -turn.z, -turn.w);
        DisplayTransform first = new DisplayTransform(new Vector3f(), turn, new Vector3f(1));
        DisplayTransform second = new DisplayTransform(new Vector3f(), negated, new Vector3f(1));

        assertTrue(first.closeTo(second, EPSILON));
        assertFalse(first.closeTo(new DisplayTransform(new Vector3f(0, 0.01f, 0), turn, new Vector3f(1)), EPSILON));
    }
}
