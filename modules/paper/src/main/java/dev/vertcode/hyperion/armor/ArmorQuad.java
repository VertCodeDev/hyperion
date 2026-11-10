package dev.vertcode.hyperion.armor;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * One face of an armor model, ready to be drawn on a limb.
 *
 * <p>Corners go round the face, in the limb's model space: pixels from the limb's pivot, Y down
 * and X to the player's left, as vanilla's boxes are. UVs are in the texture's own UV units.
 *
 * @param corners the four corners, in order round the face
 * @param uvs the UV of each corner
 * @param normal the way the face points, in the same space
 * @param texture the index of the texture the face uses
 */
public record ArmorQuad(List<Vector3fc> corners, List<Vector2fc> uvs, Vector3fc normal, int texture) {

    /**
     * Copy everything, so a quad never changes after it is made.
     */
    public ArmorQuad {
        List<Vector3fc> cornerCopies = new ArrayList<>(corners.size());
        for (Vector3fc corner : corners) {
            cornerCopies.add(new Vector3f(corner));
        }

        List<Vector2fc> uvCopies = new ArrayList<>(uvs.size());
        for (Vector2fc uv : uvs) {
            uvCopies.add(new Vector2f(uv));
        }

        corners = List.copyOf(cornerCopies);
        uvs = List.copyOf(uvCopies);
        normal = new Vector3f(normal);
    }
}
