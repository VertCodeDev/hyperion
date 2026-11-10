package dev.vertcode.hyperion.armor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.blueprint.BlockbenchRotation;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.blueprint.data.Group;
import dev.vertcode.hyperion.blueprint.data.ModelData;

/**
 * Turn an armor bone's cubes into faces in its limb's space, with every rotation applied.
 *
 * <p>The shader draws faces anywhere, so unlike an item model nothing is snapped: a cube keeps
 * its own rotation, its groups' rotations and its inflate. Blockbench's space has Y up; the
 * limb's space is vanilla's model space, with X and Y flipped around the limb's pivot, which is
 * how Blockbench exports Java entity models.
 */
public final class ArmorGeometry {

    private static final int CORNERS = 4;
    private static final int QUARTER = 90;

    private ArmorGeometry() {
    }

    /**
     * Collect the faces of a bone and every group under it that is not an armor bone itself.
     *
     * @param model the set's model
     * @param bone the bone's group
     * @param pivot the limb's pivot in Blockbench's space
     * @return the textured faces
     */
    public static List<ArmorQuad> quads(final ModelData model, final Group bone, final Vector3fc pivot) {
        List<ArmorQuad> quads = new ArrayList<>();
        collect(model, bone, List.of(), pivot, quads, true);

        return quads;
    }

    /**
     * Collect the cubes of a group and its children, with the groups that turn them.
     *
     * @param model the model
     * @param group the group
     * @param parents the groups above it, innermost first, up to the bone
     * @param pivot the limb's pivot
     * @param quads where the faces go
     * @param root whether this is the bone itself, which is collected even though it is an armor
     *     bone
     */
    private static void collect(
            final ModelData model,
            final Group group,
            final List<Group> parents,
            final Vector3fc pivot,
            final List<ArmorQuad> quads,
            final boolean root
    ) {
        if (!root && ArmorBone.fromName(group.name()).isPresent()) {
            return;
        }

        List<Group> chain = new ArrayList<>(parents.size() + 1);
        chain.add(group);
        chain.addAll(parents);
        Map<String, Cube> cubes = model.cubes();
        Map<String, Group> groups = model.groups();
        for (String child : group.children()) {
            Cube cube = cubes.get(child);
            if (cube != null) {
                if (cube.visible()) {
                    cube(cube, chain, pivot, quads);
                }

                continue;
            }

            Group sub = groups.get(child);
            if (sub != null) {
                collect(model, sub, chain, pivot, quads, false);
            }
        }
    }

    /**
     * Add one cube's textured faces.
     *
     * @param cube the cube
     * @param chain the groups that turn it, innermost first
     * @param pivot the limb's pivot
     * @param quads where the faces go
     */
    private static void cube(final Cube cube, final List<Group> chain, final Vector3fc pivot, final List<ArmorQuad> quads) {
        float grow = cube.inflate();
        Vector3f from = new Vector3f(cube.from()).sub(grow, grow, grow);
        Vector3f to = new Vector3f(cube.to()).add(grow, grow, grow);
        for (FaceSide side : FaceSide.values()) {
            CubeFace face = cube.faces().get(side);
            if (face == null || !face.textured()) {
                continue;
            }

            Vector3f[] corners = corners(side, from, to);
            List<Vector3fc> placed = new ArrayList<>(CORNERS);
            for (Vector3f corner : corners) {
                placed.add(toLimb(place(corner, cube, chain), pivot));
            }

            Vector3f normal = turn(normal(side), cube, chain);
            quads.add(new ArmorQuad(placed, uvs(face), new Vector3f(-normal.x, -normal.y, normal.z), face.texture()));
        }
    }

    /**
     * Return a face's corners, top left first going clockwise as seen from outside, the order
     * Minecraft maps a face's UV in.
     *
     * @param side the face
     * @param from the cube's lowest corner
     * @param to the cube's highest corner
     * @return the four corners
     */
    static Vector3f[] corners(final FaceSide side, final Vector3fc from, final Vector3fc to) {
        float x1 = from.x();
        float y1 = from.y();
        float z1 = from.z();
        float x2 = to.x();
        float y2 = to.y();
        float z2 = to.z();

        return switch (side) {
            case NORTH -> new Vector3f[] {v(x2, y2, z1), v(x1, y2, z1), v(x1, y1, z1), v(x2, y1, z1)};
            case SOUTH -> new Vector3f[] {v(x1, y2, z2), v(x2, y2, z2), v(x2, y1, z2), v(x1, y1, z2)};
            case EAST -> new Vector3f[] {v(x2, y2, z2), v(x2, y2, z1), v(x2, y1, z1), v(x2, y1, z2)};
            case WEST -> new Vector3f[] {v(x1, y2, z1), v(x1, y2, z2), v(x1, y1, z2), v(x1, y1, z1)};
            case UP -> new Vector3f[] {v(x1, y2, z1), v(x2, y2, z1), v(x2, y2, z2), v(x1, y2, z2)};
            case DOWN -> new Vector3f[] {v(x1, y1, z2), v(x2, y1, z2), v(x2, y1, z1), v(x1, y1, z1)};
        };
    }

    /**
     * Return the UV of each corner, in the order of {@link #corners}, with the face's rotation
     * applied: a quarter turn moves the texture one corner clockwise.
     *
     * @param face the face
     * @return the four UVs
     */
    static List<Vector2fc> uvs(final CubeFace face) {
        Vector2f[] base = {
            new Vector2f(face.u1(), face.v1()),
            new Vector2f(face.u2(), face.v1()),
            new Vector2f(face.u2(), face.v2()),
            new Vector2f(face.u1(), face.v2())
        };
        int turns = Math.floorMod(face.rotation() / QUARTER, CORNERS);
        List<Vector2fc> uvs = new ArrayList<>(CORNERS);
        for (int corner = 0; corner < CORNERS; corner++) {
            uvs.add(base[Math.floorMod(corner - turns, CORNERS)]);
        }

        return uvs;
    }

    /**
     * Turn a point by its cube's rotation, then by each group's, innermost first.
     *
     * @param point the point, in Blockbench's space
     * @param cube the cube
     * @param chain the groups
     * @return the placed point
     */
    private static Vector3f place(final Vector3fc point, final Cube cube, final List<Group> chain) {
        Vector3f placed = around(point, cube.origin(), BlockbenchRotation.of(cube.rotation()));
        for (Group group : chain) {
            placed = around(placed, group.origin(), BlockbenchRotation.of(group.rotation()));
        }

        return placed;
    }

    /**
     * Turn a direction by its cube's rotation, then by each group's.
     *
     * @param direction the direction
     * @param cube the cube
     * @param chain the groups
     * @return the turned direction
     */
    private static Vector3f turn(final Vector3fc direction, final Cube cube, final List<Group> chain) {
        Vector3f turned = BlockbenchRotation.of(cube.rotation()).transform(new Vector3f(direction));
        for (Group group : chain) {
            BlockbenchRotation.of(group.rotation()).transform(turned);
        }

        return turned;
    }

    /**
     * Turn a point around an origin.
     *
     * @param point the point
     * @param origin the origin
     * @param rotation the rotation
     * @return the turned point
     */
    private static Vector3f around(final Vector3fc point, final Vector3fc origin, final Quaternionf rotation) {
        return rotation.transform(new Vector3f(point).sub(origin)).add(origin);
    }

    /**
     * Move a point from Blockbench's space into the limb's: from its pivot, X and Y flipped.
     *
     * @param point the point, in Blockbench's space
     * @param pivot the limb's pivot, in Blockbench's space
     * @return the point in the limb's space
     */
    public static Vector3f toLimb(final Vector3fc point, final Vector3fc pivot) {
        return new Vector3f(-(point.x() - pivot.x()), -(point.y() - pivot.y()), point.z() - pivot.z());
    }

    /**
     * Return the way a face points, in Blockbench's space.
     *
     * @param side the face
     * @return the unit normal
     */
    private static Vector3f normal(final FaceSide side) {
        return switch (side) {
            case NORTH -> v(0, 0, -1);
            case SOUTH -> v(0, 0, 1);
            case EAST -> v(1, 0, 0);
            case WEST -> v(-1, 0, 0);
            case UP -> v(0, 1, 0);
            case DOWN -> v(0, -1, 0);
        };
    }

    /**
     * Make a vector.
     *
     * @param x the X value
     * @param y the Y value
     * @param z the Z value
     * @return the vector
     */
    private static Vector3f v(final float x, final float y, final float z) {
        return new Vector3f(x, y, z);
    }
}
