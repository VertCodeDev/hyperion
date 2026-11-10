package dev.vertcode.hyperion.pack.armor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import dev.vertcode.hyperion.armor.ArmorBone;
import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.blueprint.data.Group;
import dev.vertcode.hyperion.blueprint.data.ModelData;
import dev.vertcode.hyperion.blueprint.data.Texture;
import dev.vertcode.hyperion.pack.PackWriter;

/**
 * Write the item models of an armor set: the helmet as it sits on the head, and each piece's
 * inventory icon.
 *
 * <p>An item model only holds cubes between -16 and 32 pixels, turned on one axis by 0, 22.5 or
 * 45 degrees, and no group turns. A cube turned any other way is snapped to the nearest angle it
 * can have; the shader-drawn pieces are not limited like this, only the helmet and the icons
 * are.
 */
final class ArmorItemModels {

    private static final float PIXELS = 16f;
    private static final float CENTER = 8f;
    private static final float REACH = 24f;
    private static final float ICON_SIZE = 15f;
    private static final float MAX_DISPLAY_SCALE = 4f;
    private static final float ANGLE_STEP = 22.5f;
    private static final float MAX_ANGLE = 45f;
    private static final float ANGLE_EPSILON = 0.001f;
    private static final String[] AXES = {"x", "y", "z"};

    /**
     * A head is 8 pixels; a block model on the head is drawn 10 pixels wide, like a pumpkin.
     */
    private static final float HEAD_SCALE = 16f / 10f;

    /**
     * The middle of the head in Blockbench's space: 4 pixels above the neck pivot.
     */
    private static final Vector3fc HEAD_CENTER = new Vector3f(0, 28, 0);

    private final HyperionArmorSet set;
    private final PackWriter writer;
    private final JsonObject textures = new JsonObject();
    private final String namespace;
    private final String folder;

    /**
     * Prepare to write a set's item models, and write its textures.
     *
     * @param set the set
     * @param writer the pack
     */
    ArmorItemModels(final HyperionArmorSet set, final PackWriter writer) {
        this.set = set;
        this.writer = writer;
        this.namespace = set.key().namespace();
        this.folder = "armor/" + set.id();
        this.writeTextures();
    }

    /**
     * Write the helmet as it sits on the head, for the head slot and, before 1.21.4, the
     * inventory too.
     *
     * @return the model's id
     */
    String helmet() {
        List<Cube> cubes = this.cubes(List.of(ArmorBone.HELMET));
        List<Vector3f[]> boxes = new ArrayList<>();
        for (Cube cube : cubes) {
            boxes.add(new Vector3f[] {
                headPoint(new Vector3f(cube.from()).sub(cube.inflate(), cube.inflate(), cube.inflate())),
                headPoint(new Vector3f(cube.to()).add(cube.inflate(), cube.inflate(), cube.inflate()))
            });
        }

        float farthest = 0f;
        for (Vector3f[] box : boxes) {
            for (Vector3f corner : box) {
                farthest = Math.max(farthest, corner.sub(CENTER, CENTER, CENTER, new Vector3f()).absolute().maxComponent());
            }
        }

        float shrink = farthest > REACH ? REACH / farthest : 1f;
        float scale = Math.min(MAX_DISPLAY_SCALE, 1f / shrink);
        JsonObject display = new JsonObject();
        display.add("head", transform(0, 0, 0, scale));
        display.add("gui", transform(30, 225, 0, Math.min(1f, ICON_SIZE / Math.max(1f, extent(boxes) * shrink))));
        display.add("ground", transform(0, 0, 0, 0.5f));
        display.add("fixed", transform(0, 180, 0, 1f));

        return this.writeModel("helmet", cubes, point -> scaled(headPoint(point), shrink), display);
    }

    /**
     * Write a piece's inventory icon: its flat icon texture, or its cubes seen from the front
     * at an angle.
     *
     * @param piece the piece's name, such as {@code chestplate}
     * @param bones the piece's bones
     * @param icon the index of its flat icon texture, or -1 for a 3D icon
     * @return the model's id
     */
    String icon(final String piece, final List<ArmorBone> bones, final int icon) {
        String name = piece + "_icon";
        if (icon >= 0) {
            JsonObject layers = new JsonObject();
            layers.addProperty("layer0", this.textureId(icon));
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            model.add("textures", layers);
            this.writer.json(this.modelPath(name), model);

            return this.modelId(name);
        }

        List<Cube> cubes = this.cubes(bones);
        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        for (Cube cube : cubes) {
            min.min(new Vector3f(cube.from()).sub(cube.inflate(), cube.inflate(), cube.inflate()));
            max.max(new Vector3f(cube.to()).add(cube.inflate(), cube.inflate(), cube.inflate()));
        }

        Vector3f middle = cubes.isEmpty() ? new Vector3f() : new Vector3f(min).add(max).mul(0.5f);
        float size = cubes.isEmpty() ? 1f : new Vector3f(max).sub(min).maxComponent();
        float scale = Math.min(1f, PIXELS / Math.max(1f, size));
        JsonObject display = new JsonObject();
        display.add("gui", transform(30, 225, 0, 1f));
        display.add("ground", transform(0, 0, 0, 0.5f));
        display.add("fixed", transform(0, 180, 0, 1f));

        return this.writeModel(name, cubes, point -> new Vector3f(point).sub(middle).mul(scale).add(CENTER, CENTER, CENTER), display);
    }

    /**
     * Return a written model's id.
     *
     * @param name the model's file name
     * @return the id, such as {@code hyperion:armor/frost/helmet}
     */
    String modelId(final String name) {
        return this.namespace + ":" + this.folder + "/" + name;
    }

    /**
     * Write a model of cubes.
     *
     * @param name the model's file name
     * @param cubes the cubes
     * @param place moves a point from Blockbench's space into the model's
     * @param display the display transforms
     * @return the model's id
     */
    private String writeModel(
            final String name,
            final List<Cube> cubes,
            final Function<Vector3fc, Vector3f> place,
            final JsonObject display
    ) {
        JsonArray elements = new JsonArray();
        for (Cube cube : cubes) {
            elements.add(this.element(cube, place));
        }

        JsonObject model = new JsonObject();
        model.add("textures", this.textures.deepCopy());
        model.add("elements", elements);
        model.add("display", display);
        this.writer.json(this.modelPath(name), model);

        return this.modelId(name);
    }

    /**
     * Write one cube as an element, its rotation snapped to what an item model holds.
     *
     * @param cube the cube
     * @param place moves a point into the model's space
     * @return the element
     */
    private JsonObject element(final Cube cube, final Function<Vector3fc, Vector3f> place) {
        float grow = cube.inflate();
        Vector3f from = place.apply(new Vector3f(cube.from()).sub(grow, grow, grow));
        Vector3f to = place.apply(new Vector3f(cube.to()).add(grow, grow, grow));
        JsonObject element = new JsonObject();
        element.add("from", array(new Vector3f(from).min(to)));
        element.add("to", array(new Vector3f(from).max(to)));

        int axis = largestAxis(cube.rotation());
        if (axis >= 0) {
            float angle = snap(cube.rotation().get(axis));
            if (Math.abs(angle) > ANGLE_EPSILON) {
                JsonObject turn = new JsonObject();
                turn.addProperty("angle", angle);
                turn.addProperty("axis", AXES[axis]);
                turn.add("origin", array(place.apply(cube.origin())));
                element.add("rotation", turn);
            }
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
     * Write one face, its UV moved to Minecraft's 0 to 16.
     *
     * @param face the face
     * @return the face's JSON
     */
    private JsonObject face(final CubeFace face) {
        Texture texture = this.set.model().textures().get(face.texture());
        float uScale = PIXELS / Math.max(1, texture.uvWidth());
        float vScale = PIXELS / Math.max(1, texture.uvHeight());
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

        if (this.set.dyeable() && this.set.textures().tinted(face.texture())) {
            json.addProperty("tintindex", 0);
        }

        return json;
    }

    /**
     * Collect the visible cubes of some bones and the groups under them that are no armor bone.
     *
     * @param bones the bones
     * @return the cubes
     */
    private List<Cube> cubes(final List<ArmorBone> bones) {
        List<Cube> cubes = new ArrayList<>();
        Map<ArmorBone, Group> groups = this.set.bones();
        for (ArmorBone bone : bones) {
            Group group = groups.get(bone);
            if (group != null) {
                this.collect(group, cubes, true);
            }
        }

        return cubes;
    }

    /**
     * Collect a group's cubes and its children's.
     *
     * @param group the group
     * @param cubes where the cubes go
     * @param root whether this is the bone itself
     */
    private void collect(final Group group, final List<Cube> cubes, final boolean root) {
        if (!root && ArmorBone.fromName(group.name()).isPresent()) {
            return;
        }

        ModelData model = this.set.model();
        for (String child : group.children()) {
            Cube cube = model.cubes().get(child);
            if (cube != null) {
                if (cube.visible()) {
                    cubes.add(cube);
                }

                continue;
            }

            Group sub = model.groups().get(child);
            if (sub != null) {
                this.collect(sub, cubes, false);
            }
        }
    }

    /**
     * Write every texture with an image, the first frame of an animated one.
     */
    private void writeTextures() {
        List<Texture> all = this.set.model().textures();
        for (int index = 0; index < all.size(); index++) {
            Texture texture = all.get(index);
            if (texture.png().length == 0) {
                continue;
            }

            String path = "assets/" + this.namespace + "/textures/item/" + this.folder + "/" + index + ".png";
            this.writer.bytes(path, ArmorImages.png(ArmorImages.firstFrame(texture)));
            this.textures.addProperty(String.valueOf(index), this.textureId(index));
            if (!this.textures.has("particle")) {
                this.textures.addProperty("particle", this.textureId(index));
            }
        }
    }

    /**
     * Return a texture's id.
     *
     * @param index the texture's index
     * @return the id
     */
    private String textureId(final int index) {
        return this.namespace + ":item/" + this.folder + "/" + index;
    }

    /**
     * Return where a model's file goes.
     *
     * @param name the model's file name
     * @return the path in the pack
     */
    private String modelPath(final String name) {
        return "assets/" + this.namespace + "/models/" + this.folder + "/" + name + ".json";
    }

    /**
     * Move a point from Blockbench's space onto the head: around the head's middle, a pixel being
     * 1.6 model pixels.
     *
     * @param point the point
     * @return the point in the helmet model's space
     */
    private static Vector3f headPoint(final Vector3fc point) {
        return new Vector3f(point).sub(HEAD_CENTER).mul(HEAD_SCALE).add(CENTER, CENTER, CENTER);
    }

    /**
     * Shrink a point toward the model's middle.
     *
     * @param point the point
     * @param shrink the factor
     * @return the point
     */
    private static Vector3f scaled(final Vector3fc point, final float shrink) {
        return new Vector3f(point).sub(CENTER, CENTER, CENTER).mul(shrink).add(CENTER, CENTER, CENTER);
    }

    /**
     * Return the largest size of some boxes together.
     *
     * @param boxes the boxes, as lowest and highest corner
     * @return the size, in pixels
     */
    private static float extent(final List<Vector3f[]> boxes) {
        if (boxes.isEmpty()) {
            return 1f;
        }

        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(-Float.MAX_VALUE);
        for (Vector3f[] box : boxes) {
            min.min(box[0]).min(box[1]);
            max.max(box[0]).max(box[1]);
        }

        return max.sub(min).maxComponent();
    }

    /**
     * Return the axis a rotation turns most around.
     *
     * @param rotation the rotation in degrees
     * @return 0, 1 or 2, or -1 when it does not turn
     */
    private static int largestAxis(final Vector3fc rotation) {
        int axis = -1;
        float largest = ANGLE_EPSILON;
        for (int index = 0; index < 3; index++) {
            if (Math.abs(rotation.get(index)) > largest) {
                largest = Math.abs(rotation.get(index));
                axis = index;
            }
        }

        return axis;
    }

    /**
     * Snap an angle to what an item model holds: a step of 22.5 degrees, at most 45 either way.
     *
     * @param angle the angle in degrees
     * @return the snapped angle
     */
    static float snap(final float angle) {
        float snapped = Math.round(angle / ANGLE_STEP) * ANGLE_STEP;

        return Math.max(-MAX_ANGLE, Math.min(MAX_ANGLE, snapped));
    }

    /**
     * Write a display transform.
     *
     * @param x the turn around X, in degrees
     * @param y the turn around Y
     * @param z the turn around Z
     * @param scale the scale
     * @return the transform
     */
    private static JsonObject transform(final float x, final float y, final float z, final float scale) {
        JsonArray rotation = new JsonArray();
        rotation.add(x);
        rotation.add(y);
        rotation.add(z);
        JsonArray scales = new JsonArray();
        scales.add(round(scale));
        scales.add(round(scale));
        scales.add(round(scale));
        JsonObject transform = new JsonObject();
        transform.add("rotation", rotation);
        transform.add("scale", scales);

        return transform;
    }

    /**
     * Write a point as a JSON array.
     *
     * @param point the point
     * @return the array
     */
    private static JsonArray array(final Vector3fc point) {
        JsonArray array = new JsonArray();
        array.add(round(point.x()));
        array.add(round(point.y()));
        array.add(round(point.z()));

        return array;
    }

    /**
     * Round to four decimals.
     *
     * @param value the value
     * @return the rounded value
     */
    private static float round(final float value) {
        return Math.round(value * 10_000f) / 10_000f;
    }
}
