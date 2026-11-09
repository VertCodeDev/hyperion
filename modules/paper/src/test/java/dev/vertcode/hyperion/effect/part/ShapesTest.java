package dev.vertcode.hyperion.effect.part;

import java.util.List;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShapesTest {

    private static final float EPSILON = 1.0e-4f;

    @Test
    void placesARingAtItsRadius() {
        List<Vector3f> ring = Shapes.ring(12, 2f, 0.3f);

        assertEquals(12, ring.size());
        for (Vector3f point : ring) {
            assertEquals(2f, point.length(), EPSILON);
            assertEquals(0f, point.y, EPSILON);
        }
    }

    @Test
    void placesASphereAtItsRadius() {
        for (Vector3f point : Shapes.sphere(30, 1.5f)) {
            assertEquals(1.5f, point.length(), EPSILON);
        }
    }

    @Test
    void runsALineFromTheCenterToItsEnd() {
        List<Vector3f> line = Shapes.line(5, new Vector3f(0f, 0f, 4f));

        assertEquals(new Vector3f(), line.getFirst());
        assertEquals(new Vector3f(0f, 0f, 4f), line.getLast());
        assertEquals(1f, line.get(1).z, EPSILON);
    }

    @Test
    void climbsAHelixToItsHeight() {
        List<Vector3f> helix = Shapes.helix(9, 1f, 3f, 0f);

        assertEquals(0f, helix.getFirst().y, EPSILON);
        assertEquals(3f, helix.getLast().y, EPSILON);
    }

    @Test
    void readsColors() {
        assertEquals(0xFF5500, ParticleData.color("#FF5500").asRGB());
        assertEquals(0x0A141E, ParticleData.color("10, 20, 30").asRGB());
        assertEquals(0xFFFFFF, ParticleData.color("not a color").asRGB());
    }
}
