package dev.vertcode.hyperion.debug;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import dev.vertcode.hyperion.api.model.hitbox.Hitbox;

/**
 * The points the debug views put particles on: lines, box outlines and where hitboxes are.
 */
final class DebugShapes {

    private DebugShapes() {
    }

    /**
     * Return points along a line, both ends included.
     *
     * @param from where it starts
     * @param to where it ends
     * @param step the most room between two points, in blocks
     * @return the points
     */
    static List<Vector> line(final Vector from, final Vector to, final double step) {
        double length = from.distance(to);
        int gaps = Math.max(1, (int) Math.ceil(length / step));
        List<Vector> points = new ArrayList<>(gaps + 1);
        for (int i = 0; i <= gaps; i++) {
            double t = (double) i / gaps;
            points.add(new Vector(
                    from.getX() + (to.getX() - from.getX()) * t,
                    from.getY() + (to.getY() - from.getY()) * t,
                    from.getZ() + (to.getZ() - from.getZ()) * t
            ));
        }

        return points;
    }

    /**
     * Return points along the twelve edges of a box.
     *
     * @param box the box
     * @param step the most room between two points on an edge, in blocks
     * @return the points; corners are shared by three edges and come up three times
     */
    static List<Vector> edges(final BoundingBox box, final double step) {
        Vector[] corners = new Vector[8];
        for (int i = 0; i < corners.length; i++) {
            corners[i] = new Vector(
                    (i & 1) == 0 ? box.getMinX() : box.getMaxX(),
                    (i & 2) == 0 ? box.getMinY() : box.getMaxY(),
                    (i & 4) == 0 ? box.getMinZ() : box.getMaxZ()
            );
        }

        List<Vector> points = new ArrayList<>();
        for (int i = 0; i < corners.length; i++) {
            for (int bit = 1; bit < corners.length; bit <<= 1) {
                // Each edge joins two corners one bit apart; walk it from the lower one.
                if ((i & bit) == 0) {
                    points.addAll(line(corners[i], corners[i | bit], step));
                }
            }
        }

        return points;
    }

    /**
     * Return the box an interaction entity covers: centred on its spot, standing on it.
     *
     * @param base the entity's spot
     * @param width its width
     * @param height its height
     * @return the box
     */
    static BoundingBox interaction(final Vector base, final double width, final double height) {
        double half = width / 2d;

        return new BoundingBox(
                base.getX() - half,
                base.getY(),
                base.getZ() - half,
                base.getX() + half,
                base.getY() + height,
                base.getZ() + half
        );
    }

    /**
     * Return the box a hitbox bone covers, placed the way its interaction is.
     *
     * @param bone where the bone is
     * @param rotation the bone's rotation in the world
     * @param hitbox the bone's hitbox
     * @param scale the model's scale
     * @return the box
     */
    static BoundingBox hitbox(final Vector bone, final Quaternionfc rotation, final Hitbox hitbox, final float scale) {
        Vector3f offset = rotation.transform(new Vector3f(hitbox.offset())).mul(scale);
        Vector base = bone.clone().add(new Vector(offset.x, offset.y, offset.z));

        return interaction(base, hitbox.width() * scale, hitbox.height() * scale);
    }

    /**
     * Return where a line from a point along a turned axis ends.
     *
     * @param from where it starts
     * @param rotation the rotation
     * @param axis the axis before turning, such as {@code (1, 0, 0)}
     * @param length its length, in blocks
     * @return the end
     */
    static Vector along(final Vector from, final Quaternionfc rotation, final Vector3f axis, final float length) {
        Vector3f turned = rotation.transform(new Vector3f(axis)).mul(length);

        return from.clone().add(new Vector(turned.x, turned.y, turned.z));
    }
}
