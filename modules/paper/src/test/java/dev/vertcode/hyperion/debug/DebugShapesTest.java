package dev.vertcode.hyperion.debug;

import java.util.List;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.vertcode.hyperion.api.model.hitbox.Hitbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugShapesTest {

    private static final double EPSILON = 1.0e-5d;

    @Test
    void walksALineWithBothEnds() {
        List<Vector> points = DebugShapes.line(new Vector(0, 0, 0), new Vector(1, 0, 0), 0.25d);

        assertEquals(5, points.size());
        assertTrue(points.getFirst().equals(new Vector(0, 0, 0)));
        assertTrue(points.getLast().distance(new Vector(1, 0, 0)) < EPSILON);
    }

    @Test
    void outlinesTheTwelveEdgesOfABox() {
        List<Vector> points = DebugShapes.edges(new BoundingBox(0, 0, 0, 1, 1, 1), 1d);

        // Each edge is one gap long here, so two points for each of the twelve.
        assertEquals(24, points.size());
        for (Vector point : points) {
            int onFaces = 0;
            onFaces += point.getX() == 0d || point.getX() == 1d ? 1 : 0;
            onFaces += point.getY() == 0d || point.getY() == 1d ? 1 : 0;
            onFaces += point.getZ() == 0d || point.getZ() == 1d ? 1 : 0;
            assertTrue(onFaces >= 2, "every point lies on an edge");
        }
    }

    @Test
    void standsAnInteractionOnItsSpot() {
        BoundingBox box = DebugShapes.interaction(new Vector(10, 64, -3), 2d, 3d);

        assertEquals(9d, box.getMinX(), EPSILON);
        assertEquals(64d, box.getMinY(), EPSILON);
        assertEquals(67d, box.getMaxY(), EPSILON);
        assertEquals(-2d, box.getMaxZ(), EPSILON);
    }

    @Test
    void turnsAndScalesAHitboxOffset() {
        Quaternionf quarter = new Quaternionf().rotationY((float) Math.toRadians(90d));
        Hitbox hitbox = new Hitbox(1f, 2f, new Vector3f(1f, 0f, 0f));
        BoundingBox box = DebugShapes.hitbox(new Vector(0, 0, 0), quarter, hitbox, 2f);

        // A quarter turn around Y takes +X to -Z; the scale doubles offset and size.
        assertEquals(0d, box.getCenterX(), EPSILON);
        assertEquals(-2d, box.getCenterZ(), EPSILON);
        assertEquals(2d, box.getWidthX(), EPSILON);
        assertEquals(4d, box.getHeight(), EPSILON);
    }

    @Test
    void pointsAlongATurnedAxis() {
        Quaternionf quarter = new Quaternionf().rotationY((float) Math.toRadians(90d));
        Vector end = DebugShapes.along(new Vector(1, 2, 3), quarter, new Vector3f(1f, 0f, 0f), 0.5f);

        assertTrue(end.distance(new Vector(1, 2, 2.5)) < EPSILON);
    }
}
