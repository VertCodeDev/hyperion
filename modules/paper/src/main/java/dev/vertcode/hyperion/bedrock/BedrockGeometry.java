package dev.vertcode.hyperion.bedrock;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.util.BoundingBox;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;

/**
 * Write a model as Bedrock entity geometry.
 *
 * <p>Bedrock geometry is what Blockbench models are made in, so every bone and cube carries over:
 * any rotation, any size, nothing split or shrunk. Bedrock mirrors X against Blockbench's own
 * space, as Blockbench does when it exports: a cube starts at {@code -to.x}, and X and Y angles
 * turn the other way.
 */
final class BedrockGeometry {

    private static final float PIXELS_PER_BLOCK = 16f;

    private BedrockGeometry() {
    }

    /**
     * Write a blueprint's geometry.
     *
     * @param blueprint the blueprint
     * @param slug the model's Bedrock name
     * @param atlas the model's textures, which faces' UVs point into
     * @param hidden tells which bones' own cubes to leave out
     * @return the geometry file
     */
    static JsonObject of(
            final Blueprint blueprint,
            final String slug,
            final BedrockAtlas atlas,
            final Predicate<BoneBlueprint> hidden
    ) {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.hyperion." + slug);
        description.addProperty("texture_width", atlas.width());
        description.addProperty("texture_height", atlas.height());
        BoundingBox box = blueprint.boundingBox();
        double reach = Math.max(Math.max(Math.abs(box.getMinX()), Math.abs(box.getMaxX())),
                Math.max(Math.abs(box.getMinZ()), Math.abs(box.getMaxZ())));
        float height = (float) Math.max(1d, box.getMaxY() + 1d);
        description.addProperty("visible_bounds_width", (float) Math.max(1d, reach * 2d + 1d));
        description.addProperty("visible_bounds_height", height);
        description.add("visible_bounds_offset", vector(0f, height / 2f, 0f));

        JsonArray bones = new JsonArray();
        for (BlueprintBone bone : blueprint.blueprintBones()) {
            bones.add(bone(bone, atlas, hidden.test(bone)));
        }

        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);
        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.16.0");
        file.add("minecraft:geometry", geometries);

        return file;
    }

    /**
     * Write one bone and its cubes.
     *
     * @param bone the bone
     * @param atlas the model's textures
     * @param hidden true to leave the bone's own cubes out, keeping it for its children
     * @return the bone
     */
    private static JsonObject bone(final BlueprintBone bone, final BedrockAtlas atlas, final boolean hidden) {
        JsonObject json = new JsonObject();
        json.addProperty("name", bone.name());
        Optional<BoneBlueprint> parent = bone.parent();
        parent.ifPresent(up -> json.addProperty("parent", up.name()));
        Vector3fc pivot = bone.pivot();
        json.add("pivot", vector(-pivot.x() * PIXELS_PER_BLOCK, pivot.y() * PIXELS_PER_BLOCK, pivot.z() * PIXELS_PER_BLOCK));
        Vector3fc angles = bone.angles();
        if (angles.lengthSquared() > 0f) {
            json.add("rotation", vector(-angles.x(), -angles.y(), angles.z()));
        }

        JsonArray cubes = new JsonArray();
        for (Cube cube : bone.cubes()) {
            if (cube.visible() && !hidden) {
                cubes.add(cube(cube, atlas));
            }
        }

        if (!cubes.isEmpty()) {
            json.add("cubes", cubes);
        }

        return json;
    }

    /**
     * Write one cube.
     *
     * @param cube the cube, in Blockbench pixels
     * @param atlas the model's textures
     * @return the cube
     */
    private static JsonObject cube(final Cube cube, final BedrockAtlas atlas) {
        Vector3fc from = cube.from();
        Vector3fc to = cube.to();
        JsonObject json = new JsonObject();
        json.add("origin", vector(-to.x(), from.y(), from.z()));
        json.add("size", vector(to.x() - from.x(), to.y() - from.y(), to.z() - from.z()));
        if (cube.inflate() != 0f) {
            json.addProperty("inflate", cube.inflate());
        }

        Vector3fc rotation = cube.rotation();
        if (rotation.lengthSquared() > 0f) {
            Vector3fc origin = cube.origin();
            json.add("pivot", vector(-origin.x(), origin.y(), origin.z()));
            json.add("rotation", vector(-rotation.x(), -rotation.y(), rotation.z()));
        }

        JsonObject faces = new JsonObject();
        for (Map.Entry<FaceSide, CubeFace> face : cube.faces().entrySet()) {
            float[] uv = face.getValue().textured() ? atlas.uv(face.getValue()) : null;
            if (uv == null) {
                continue;
            }

            JsonObject mapped = new JsonObject();
            boolean flat = face.getKey() == FaceSide.UP || face.getKey() == FaceSide.DOWN;
            if (flat) {
                // Bedrock reads the top and bottom faces from the far corner, as Blockbench writes them.
                mapped.add("uv", pair(uv[0] + uv[2], uv[1] + uv[3]));
                mapped.add("uv_size", pair(-uv[2], -uv[3]));
            } else {
                mapped.add("uv", pair(uv[0], uv[1]));
                mapped.add("uv_size", pair(uv[2], uv[3]));
            }

            faces.add(face.getKey().key(), mapped);
        }

        json.add("uv", faces);

        return json;
    }

    private static JsonArray vector(final float x, final float y, final float z) {
        JsonArray array = new JsonArray();
        array.add(round(x));
        array.add(round(y));
        array.add(round(z));

        return array;
    }

    private static JsonArray pair(final float first, final float second) {
        JsonArray array = new JsonArray();
        array.add(round(first));
        array.add(round(second));

        return array;
    }

    /**
     * Round to four decimals, so the same model writes the same file.
     *
     * @param value the value
     * @return the rounded value
     */
    private static float round(final float value) {
        return Math.round(value * 10_000f) / 10_000f;
    }
}
