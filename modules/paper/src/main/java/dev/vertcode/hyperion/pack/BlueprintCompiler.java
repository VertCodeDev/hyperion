package dev.vertcode.hyperion.pack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.blueprint.BlockbenchRotation;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.blueprint.BlueprintBone;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * Write one blueprint into a pack: its textures, one item model per bone, and the parts each
 * bone is drawn with.
 *
 * <p>An item model only holds cubes between -16 and 32 pixels, turned on one axis by 0, 22.5 or
 * 45 degrees. Each bone becomes a model centered on its pivot; a bone that does not fit is shrunk
 * and its part scaled back up, and a cube turned any other way becomes a part of its own, turned
 * by the display. Those limits are the ones 1.21 has, so the result renders the same on every
 * supported version.
 */
final class BlueprintCompiler {

    private static final float PIXELS_PER_BLOCK = 16f;
    private static final float CENTER = 8f;
    private static final float REACH = 24f;
    private static final float ANGLE_EPSILON = 0.001f;
    private static final float[] ITEM_ANGLES = {-45f, -22.5f, 22.5f, 45f};
    private static final String[] AXES = {"x", "y", "z"};
    private static final Pattern UNSAFE = Pattern.compile("[^a-z0-9_.-]");

    private final Blueprint blueprint;
    private final PackWriter writer;
    private final boolean itemDefinitions;
    private final IntSupplier customModelData;
    private final List<ModelOverride> overrides;
    private final String namespace;
    private final String folder;
    private final JsonObject textures = new JsonObject();
    private final Set<String> names = new HashSet<>();

    /**
     * Prepare to write one blueprint.
     *
     * @param blueprint the blueprint
     * @param writer the pack being written
     * @param itemDefinitions whether the server reads item definitions, from 1.21.4
     * @param customModelData hands out the next custom model data number, before 1.21.4
     * @param overrides where custom model data overrides are collected, before 1.21.4
     */
    BlueprintCompiler(
            final Blueprint blueprint,
            final PackWriter writer,
            final boolean itemDefinitions,
            final IntSupplier customModelData,
            final List<ModelOverride> overrides
    ) {
        this.blueprint = blueprint;
        this.writer = writer;
        this.itemDefinitions = itemDefinitions;
        this.customModelData = customModelData;
        this.overrides = overrides;
        this.namespace = blueprint.key().namespace();
        this.folder = blueprint.key().value();
    }

    /**
     * Write the blueprint.
     *
     * @return the parts of every bone with cubes, by bone name
     */
    Map<String, List<ModelPart>> compile() {
        this.writeTextures();

        Map<String, List<ModelPart>> parts = new LinkedHashMap<>();
        for (BlueprintBone bone : this.blueprint.bones()) {
            if (!bone.cubes().isEmpty()) {
                parts.put(bone.name(), this.bone(bone));
            }
        }

        return parts;
    }

    /**
     * Write every texture with an image, with a {@code .mcmeta} for an animated one.
     */
    private void writeTextures() {
        List<Texture> all = this.blueprint.textures();
        for (int index = 0; index < all.size(); index++) {
            Texture texture = all.get(index);
            if (texture.png().length == 0) {
                continue;
            }

            String path = "assets/" + this.namespace + "/textures/item/" + this.folder + "/" + index + ".png";
            this.writer.bytes(path, texture.png());
            this.textures.addProperty(String.valueOf(index), this.namespace + ":item/" + this.folder + "/" + index);
            if (!this.textures.has("particle")) {
                this.textures.addProperty("particle", this.namespace + ":item/" + this.folder + "/" + index);
            }

            if (texture.frames() > 1) {
                this.writer.json(path + ".mcmeta", animation(texture));
            }
        }
    }

    /**
     * Write a bone's models: one for the cubes an item model can hold as they are, and one for
     * each cube it cannot.
     *
     * @param bone the bone
     * @return the bone's parts
     */
    private List<ModelPart> bone(final BlueprintBone bone) {
        String name = this.unique(bone.name());
        Vector3f pivot = new Vector3f(bone.pivot()).mul(PIXELS_PER_BLOCK);
        List<Cube> together = new ArrayList<>();
        List<Cube> apart = new ArrayList<>();
        for (Cube cube : bone.cubes()) {
            (fitsItemModel(cube.rotation()) ? together : apart).add(cube);
        }

        List<ModelPart> parts = new ArrayList<>();
        if (!together.isEmpty()) {
            parts.add(this.part(name, together, pivot, true, new Vector3f(), new Quaternionf()));
        }

        for (int i = 0; i < apart.size(); i++) {
            Cube cube = apart.get(i);
            Vector3f offset = new Vector3f(cube.origin()).sub(pivot).div(PIXELS_PER_BLOCK);
            Quaternionf rotation = BlockbenchRotation.of(cube.rotation());
            String partName = this.unique(name + "_" + (i + 1));
            parts.add(this.part(partName, List.of(cube), cube.origin(), false, offset, rotation));
        }

        return parts;
    }

    /**
     * Write one item model, and its item definition or override, and describe the part.
     *
     * @param name the model's file name
     * @param cubes the cubes it holds
     * @param origin the point in the model's center, in pixels
     * @param keepRotation whether the cubes keep their own rotation; false when the display turns
     *     the part instead
     * @param offset where the part sits from the bone's pivot, in blocks
     * @param rotation how the display turns the part
     * @return the part
     */
    private ModelPart part(
            final String name,
            final List<Cube> cubes,
            final Vector3fc origin,
            final boolean keepRotation,
            final Vector3fc offset,
            final Quaternionfc rotation
    ) {
        float shrink = shrink(cubes, origin);
        JsonArray elements = new JsonArray();
        for (Cube cube : cubes) {
            elements.add(this.element(cube, origin, shrink, keepRotation));
        }

        JsonObject model = new JsonObject();
        model.add("textures", this.textures.deepCopy());
        model.add("elements", elements);
        String id = this.namespace + ":" + this.folder + "/" + name;
        this.writer.json("assets/" + this.namespace + "/models/" + this.folder + "/" + name + ".json", model);

        int number = 0;
        if (this.itemDefinitions) {
            String path = "assets/" + this.namespace + "/items/" + this.folder + "/" + name + ".json";
            this.writer.json(path, definition(id));
        } else {
            number = this.customModelData.getAsInt();
            this.overrides.add(new ModelOverride(number, id));
        }

        return new ModelPart(id, number, offset, rotation, 1f / shrink);
    }

    /**
     * Write one cube as an item model element.
     *
     * @param cube the cube, in pixels
     * @param origin the point in the model's center, in pixels
     * @param shrink how much the model is shrunk to fit
     * @param keepRotation whether to write the cube's rotation
     * @return the element
     */
    private JsonObject element(
            final Cube cube,
            final Vector3fc origin,
            final float shrink,
            final boolean keepRotation
    ) {
        float inflate = cube.inflate();
        JsonObject element = new JsonObject();
        element.add("from", position(new Vector3f(cube.from()).sub(inflate, inflate, inflate), origin, shrink));
        element.add("to", position(new Vector3f(cube.to()).add(inflate, inflate, inflate), origin, shrink));

        int axis = turnedAxis(cube.rotation());
        if (keepRotation && axis >= 0) {
            JsonObject turn = new JsonObject();
            turn.addProperty("angle", cube.rotation().get(axis));
            turn.addProperty("axis", AXES[axis]);
            turn.add("origin", position(cube.origin(), origin, shrink));
            element.add("rotation", turn);
        }

        JsonObject faces = new JsonObject();
        for (FaceSide side : FaceSide.values()) {
            CubeFace face = cube.faces().get(side);
            if (face != null && face.textured() && this.textures.has(String.valueOf(face.texture()))) {
                faces.add(side.key(), this.face(face));
            }
        }

        element.add("faces", faces);

        return element;
    }

    /**
     * Write one face, with its UVs moved from the texture's UV size to Minecraft's 0 to 16.
     *
     * @param face the face
     * @return the face's JSON
     */
    private JsonObject face(final CubeFace face) {
        Texture texture = this.blueprint.textures().get(face.texture());
        float uScale = PIXELS_PER_BLOCK / Math.max(1, texture.uvWidth());
        float vScale = PIXELS_PER_BLOCK / Math.max(1, texture.uvHeight());

        JsonArray uv = new JsonArray();
        uv.add(round(face.u1() * uScale));
        uv.add(round(face.v1() * vScale));
        uv.add(round(face.u2() * uScale));
        uv.add(round(face.v2() * vScale));

        JsonObject json = new JsonObject();
        json.add("uv", uv);
        json.addProperty("texture", "#" + face.texture());
        if (face.rotation() != 0) {
            json.addProperty("rotation", face.rotation());
        }

        json.addProperty("tintindex", 0);

        return json;
    }

    /**
     * Return a model name for a bone that is safe in a resource path and not used yet.
     *
     * @param name the bone's name
     * @return the file name
     */
    private String unique(final String name) {
        String safe = UNSAFE.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("_");
        String unique = safe;
        int number = 2;
        while (!this.names.add(unique)) {
            unique = safe + "_" + number++;
        }

        return unique;
    }

    /**
     * Return whether an item model can hold a rotation as it is: none, or one axis turned by
     * 22.5 or 45 degrees either way.
     *
     * @param rotation the rotation in degrees
     * @return true when the cube can keep its rotation inside the model
     */
    static boolean fitsItemModel(final Vector3fc rotation) {
        int turned = 0;
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(rotation.get(axis)) > ANGLE_EPSILON) {
                turned++;
            }
        }

        if (turned == 0) {
            return true;
        }

        if (turned > 1) {
            return false;
        }

        float angle = rotation.get(turnedAxis(rotation));
        for (float allowed : ITEM_ANGLES) {
            if (Math.abs(angle - allowed) < ANGLE_EPSILON) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the one axis a rotation turns.
     *
     * @param rotation the rotation in degrees
     * @return 0, 1 or 2 for X, Y or Z, or -1 when it turns none
     */
    private static int turnedAxis(final Vector3fc rotation) {
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(rotation.get(axis)) > ANGLE_EPSILON) {
                return axis;
            }
        }

        return -1;
    }

    /**
     * Work out how much cubes must shrink to stay within an item model's reach of its center.
     *
     * @param cubes the cubes, in pixels
     * @param origin the point in the model's center
     * @return 1 when they fit, otherwise the factor below 1 that makes them fit
     */
    private static float shrink(final List<Cube> cubes, final Vector3fc origin) {
        float farthest = 0f;
        for (Cube cube : cubes) {
            float inflate = cube.inflate();
            for (int axis = 0; axis < 3; axis++) {
                farthest = Math.max(farthest, Math.abs(cube.from().get(axis) - inflate - origin.get(axis)));
                farthest = Math.max(farthest, Math.abs(cube.to().get(axis) + inflate - origin.get(axis)));
            }
        }

        return farthest > REACH ? REACH / farthest : 1f;
    }

    /**
     * Move a point from the model's space into the item model's, around its center.
     *
     * @param point the point, in pixels
     * @param origin the point that lands in the center
     * @param shrink how much the model is shrunk
     * @return the position as a JSON array
     */
    private static JsonArray position(final Vector3fc point, final Vector3fc origin, final float shrink) {
        JsonArray array = new JsonArray();
        for (int axis = 0; axis < 3; axis++) {
            array.add(round((point.get(axis) - origin.get(axis)) * shrink + CENTER));
        }

        return array;
    }

    /**
     * Write an item definition that draws a model, tinted by the item's dye color.
     *
     * @param model the model's id
     * @return the definition
     */
    private static JsonObject definition(final String model) {
        JsonObject tint = new JsonObject();
        tint.addProperty("type", "minecraft:dye");
        tint.addProperty("default", -1);
        JsonArray tints = new JsonArray();
        tints.add(tint);

        JsonObject drawn = new JsonObject();
        drawn.addProperty("type", "minecraft:model");
        drawn.addProperty("model", model);
        drawn.add("tints", tints);

        JsonObject definition = new JsonObject();
        definition.add("model", drawn);

        return definition;
    }

    /**
     * Write the {@code .mcmeta} of an animated texture.
     *
     * @param texture the texture
     * @return the file's contents
     */
    private static JsonObject animation(final Texture texture) {
        JsonObject animation = new JsonObject();
        animation.addProperty("frametime", texture.frameTime());
        if (texture.frameInterpolate()) {
            animation.addProperty("interpolate", true);
        }

        if (!texture.frameOrder().isEmpty()) {
            JsonArray frames = new JsonArray();
            texture.frameOrder().forEach(frames::add);
            animation.add("frames", frames);
        }

        JsonObject meta = new JsonObject();
        meta.add("animation", animation);

        return meta;
    }

    /**
     * Round to four decimals, which is finer than a pixel can show and keeps the files small.
     *
     * @param value the value
     * @return the rounded value
     */
    private static float round(final float value) {
        return Math.round(value * 10_000f) / 10_000f;
    }
}
