package dev.vertcode.hyperion.animation;

import java.util.List;
import java.util.Random;

import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.animation.molang.MolangContext;
import dev.vertcode.hyperion.animation.molang.MolangExpression;
import dev.vertcode.hyperion.blueprint.data.Bezier;
import dev.vertcode.hyperion.blueprint.data.Interpolation;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyframeCurveTest {

    private static final float EPSILON = 1.0e-3f;
    private static final MolangContext CONTEXT = new MolangContext(new Random(1));

    @Test
    void holdsTheEndValuesOutsideTheKeyframes() {
        KeyframeCurve curve = new KeyframeCurve(List.of(key(1f, 10f, Interpolation.LINEAR), key(2f, 20f, Interpolation.LINEAR)));

        assertEquals(10f, at(curve, 0f), EPSILON);
        assertEquals(20f, at(curve, 5f), EPSILON);
    }

    @Test
    void drawsAStraightLineBetweenLinearKeyframes() {
        KeyframeCurve curve = new KeyframeCurve(List.of(key(0f, 0f, Interpolation.LINEAR), key(1f, 90f, Interpolation.LINEAR)));

        assertEquals(45f, at(curve, 0.5f), EPSILON);
        assertEquals(22.5f, at(curve, 0.25f), EPSILON);
    }

    @Test
    void holdsAStepKeyframeUntilTheNext() {
        KeyframeCurve curve = new KeyframeCurve(List.of(key(0f, 5f, Interpolation.STEP), key(1f, 50f, Interpolation.LINEAR)));

        assertEquals(5f, at(curve, 0.99f), EPSILON);
        assertEquals(50f, at(curve, 1f), EPSILON);
    }

    @Test
    void curvesThroughTheKeyframesAroundASmoothSegment() {
        KeyframeCurve curve = new KeyframeCurve(List.of(
                key(0f, 0f, Interpolation.CATMULLROM),
                key(1f, 10f, Interpolation.CATMULLROM),
                key(2f, 0f, Interpolation.CATMULLROM)
        ));

        assertEquals(KeyframeCurve.catmullRom(0.5f, 0f, 0f, 10f, 0f), at(curve, 0.5f), EPSILON);
        assertEquals(10f, at(curve, 1f), EPSILON);
    }

    @Test
    void followsABezierWithoutHandlesInAStraightLine() {
        Bezier flat = new Bezier(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f());
        KeyframeCurve curve = new KeyframeCurve(List.of(
                key(0f, 0f, Interpolation.BEZIER, flat),
                key(1f, 10f, Interpolation.BEZIER, flat)
        ));

        assertEquals(5f, at(curve, 0.5f), 0.01f);
        assertEquals(2.5f, at(curve, 0.25f), 0.01f);
    }

    private static float at(final KeyframeCurve curve, final float time) {
        float[] into = new float[3];
        curve.value(time, CONTEXT, into, 0);

        return into[0];
    }

    private static CurveKey key(final float time, final float value, final Interpolation interpolation) {
        return key(time, value, interpolation, null);
    }

    private static CurveKey key(
            final float time,
            final float value,
            final Interpolation interpolation,
            final @Nullable Bezier bezier
    ) {
        MolangExpression constant = context -> value;
        MolangExpression[] axes = {constant, constant, constant};

        return new CurveKey(time, interpolation, axes, axes, bezier);
    }
}
