package dev.vertcode.hyperion.blueprint.data;

import java.util.Map;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A cube from a Blockbench file, in Blockbench pixels relative to the model's origin.
 *
 * @param name the cube's name in the outliner
 * @param from the corner with the smallest coordinates
 * @param to the corner with the largest coordinates
 * @param origin the point the cube turns around
 * @param rotation the rotation in degrees around X, Y and Z, applied in Blockbench's ZYX order
 * @param inflate how far every face is pushed out, in pixels
 * @param faces the faces Blockbench saved; a missing face is not rendered
 * @param visible whether the cube was visible when the model was saved
 */
public record Cube(
        String name,
        Vector3fc from,
        Vector3fc to,
        Vector3fc origin,
        Vector3fc rotation,
        float inflate,
        Map<FaceSide, CubeFace> faces,
        boolean visible
) {

    /**
     * Copy every vector and the face map, so a cube never changes after it is read.
     */
    public Cube {
        from = new Vector3f(from);
        to = new Vector3f(to);
        origin = new Vector3f(origin);
        rotation = new Vector3f(rotation);
        faces = Map.copyOf(faces);
    }
}
