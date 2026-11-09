package dev.vertcode.hyperion.effect.part;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.joml.Vector3f;

/**
 * The shapes a particle emitter draws, as points around its center, in blocks to the right, up
 * and forward.
 *
 * <p>Every shape is the same for the same tick, so an effect looks the same each time it plays.
 * A ring and a helix turn as the ticks go by.
 */
public final class Shapes {

    private static final float FULL_TURN = (float) (Math.PI * 2d);
    private static final float GOLDEN_ANGLE = (float) (Math.PI * (3d - Math.sqrt(5d)));
    private static final float HELIX_TURNS = 2f;

    private Shapes() {
    }

    /**
     * The shapes.
     */
    public enum Shape {

        /**
         * Every particle at the center, scattered by the spread.
         */
        POINT,

        /**
         * A flat circle.
         */
        RING,

        /**
         * A spiral two turns high.
         */
        HELIX,

        /**
         * A ball, its points spread evenly.
         */
        SPHERE,

        /**
         * A line from the center to {@code to}.
         */
        LINE;

        /**
         * Find a shape by name.
         *
         * @param name the name, any case
         * @return the shape, a point for an unknown name
         */
        public static Shape of(final String name) {
            try {
                return valueOf(name.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return POINT;
            }
        }
    }

    /**
     * Work out a ring's points.
     *
     * @param count how many points
     * @param radius the radius in blocks
     * @param phase how far it has turned, in radians
     * @return the points
     */
    public static List<Vector3f> ring(final int count, final float radius, final float phase) {
        List<Vector3f> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float angle = phase + FULL_TURN * i / count;
            points.add(new Vector3f((float) Math.cos(angle) * radius, 0f, (float) Math.sin(angle) * radius));
        }

        return points;
    }

    /**
     * Work out a helix's points, from the center up.
     *
     * @param count how many points
     * @param radius the radius in blocks
     * @param height how high it rises, in blocks
     * @param phase how far it has turned, in radians
     * @return the points
     */
    public static List<Vector3f> helix(final int count, final float radius, final float height, final float phase) {
        List<Vector3f> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float along = count == 1 ? 0f : (float) i / (count - 1);
            float angle = phase + along * FULL_TURN * HELIX_TURNS;
            points.add(new Vector3f((float) Math.cos(angle) * radius, along * height, (float) Math.sin(angle) * radius));
        }

        return points;
    }

    /**
     * Work out a sphere's points, spread evenly by the golden angle.
     *
     * @param count how many points
     * @param radius the radius in blocks
     * @return the points
     */
    public static List<Vector3f> sphere(final int count, final float radius) {
        List<Vector3f> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float y = 1f - 2f * (i + 0.5f) / count;
            float around = (float) Math.sqrt(Math.max(0f, 1f - y * y));
            float angle = GOLDEN_ANGLE * i;
            points.add(new Vector3f(
                    (float) Math.cos(angle) * around * radius,
                    y * radius,
                    (float) Math.sin(angle) * around * radius
            ));
        }

        return points;
    }

    /**
     * Work out a line's points, from the center to its end, both included.
     *
     * @param count how many points
     * @param end the far end
     * @return the points
     */
    public static List<Vector3f> line(final int count, final Vector3f end) {
        List<Vector3f> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float along = count == 1 ? 0f : (float) i / (count - 1);
            points.add(new Vector3f(end).mul(along));
        }

        return points;
    }
}
