package dev.vertcode.hyperion.blueprint.bbmodel;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.metadata.Metadata;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.data.Animation;
import dev.vertcode.hyperion.blueprint.data.Animator;
import dev.vertcode.hyperion.blueprint.data.AnimatorType;
import dev.vertcode.hyperion.blueprint.data.Bezier;
import dev.vertcode.hyperion.blueprint.data.Cube;
import dev.vertcode.hyperion.blueprint.data.CubeFace;
import dev.vertcode.hyperion.blueprint.data.FaceSide;
import dev.vertcode.hyperion.blueprint.data.Group;
import dev.vertcode.hyperion.blueprint.data.Interpolation;
import dev.vertcode.hyperion.blueprint.data.Keyframe;
import dev.vertcode.hyperion.blueprint.data.KeyframeChannel;
import dev.vertcode.hyperion.blueprint.data.KeyframePoint;
import dev.vertcode.hyperion.blueprint.data.Locator;
import dev.vertcode.hyperion.blueprint.data.LoopMode;
import dev.vertcode.hyperion.blueprint.data.ModelData;
import dev.vertcode.hyperion.blueprint.data.Texture;
import dev.vertcode.hyperion.blueprint.metadata.JsonMetadata;

/**
 * Turn the JSON of a {@code .bbmodel} file into {@link ModelData}.
 *
 * <p>The reader is lenient: whatever it cannot use becomes a warning in the report and the rest
 * of the model still loads. It reads both outliner layouts Blockbench has saved: before 5.0 the
 * groups sit inside {@code outliner} with all their fields, from 5.0 they sit in a top-level
 * {@code groups} list and the outliner only holds their ids and children.
 *
 * <p>Fields Blockbench itself uses are dropped; every other field on the project, a group or a
 * locator is kept as metadata, which is how Blockbench plugins reach the server.
 */
public final class BbmodelReader {

    private static final Set<String> PROJECT_FIELDS = Set.of(
            "meta", "name", "model_identifier", "parent", "visible_box", "variable_placeholders",
            "variable_placeholder_buttons", "unhandled_root_fields", "resolution", "elements",
            "outliner", "groups", "textures", "animations", "animation_controllers",
            "animation_variable_placeholders", "timeline_setups", "multi_file_ruleset",
            "bedrock_animation_mode", "export_options", "java_block_version", "ambientocclusion",
            "front_gui_light", "display", "backgrounds", "reference_images", "history",
            "history_index", "fabricOptions", "geometry_name", "texture_groups", "overrides"
    );

    private static final Set<String> GROUP_FIELDS = Set.of(
            "name", "uuid", "export", "locked", "selected", "primary_selected", "origin", "rotation",
            "color", "children", "reset", "shade", "mirror_uv", "visibility", "autouv", "isOpen",
            "scope", "_static", "bedrock_binding", "cem_animations", "cem_attach", "cem_scale",
            "cem_model", "texture", "skin_original_origin", "material"
    );

    private static final Set<String> LOCATOR_FIELDS = Set.of(
            "name", "uuid", "type", "position", "rotation", "ignore_inherited_scale", "visibility",
            "locked", "selected", "export", "scope", "ik_target", "lock_ik_target_rotation",
            "ik_source", "color"
    );

    private static final Set<String> KNOWN_FORMATS = Set.of("free", "bedrock", "bedrock_old", "java_block");

    private static final String PNG_PREFIX = "data:image/png;base64,";
    private static final long PNG_SIGNATURE = 0x89504E470D0A1A0AL;
    private static final int PNG_HEADER_LENGTH = 24;
    private static final int PNG_WIDTH_OFFSET = 16;
    private static final String EFFECTS_TARGET = "effects";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static final String UNKNOWN_FORMAT = "Blockbench format '%s' is not one Hyperion is tested with; "
            + "use Generic Model or Bedrock Entity if anything looks wrong";
    private static final String UNRENDERED_ELEMENT = "is a %s, which Hyperion does not render, so it is left out";
    private static final String MISSING_GROUP = "the outliner points at group %s, which is not in the file";
    private static final String MISSING_TEXTURE = "face %s uses texture %d, but the file has %d texture(s)";
    private static final String NO_IMAGE = "has no image saved in the file; save the texture into the project";
    private static final String BAD_IMAGE = "has image data that is not a readable PNG";
    private static final String UNKNOWN_LOOP = "has loop mode '%s', which Hyperion does not know; it plays once";
    private static final String UNKNOWN_ANIMATOR = "animates a %s named '%s', which Hyperion does not animate";
    private static final String UNKNOWN_CHANNEL = "has a '%s' keyframe on '%s', which Hyperion does not know";
    private static final String UNKNOWN_INTERPOLATION = "has '%s' interpolation on '%s'; it is played as linear";

    private final JsonObject json;
    private final LoadReport report;
    private final List<Texture> textures = new ArrayList<>();
    private final Map<String, Cube> cubes = new LinkedHashMap<>();
    private final Map<String, Locator> locators = new LinkedHashMap<>();
    private final Map<String, Group> groups = new LinkedHashMap<>();
    private final Map<String, JsonObject> groupFields = new HashMap<>();

    private BbmodelReader(final JsonObject json, final LoadReport report) {
        this.json = json;
        this.report = report;
    }

    /**
     * Read a Blockbench project.
     *
     * @param json the parsed {@code .bbmodel} file
     * @param report where problems are written; check it for errors before using the result
     * @return everything the file holds that Hyperion uses
     */
    public static ModelData read(final JsonObject json, final LoadReport report) {
        return new BbmodelReader(json, report).read();
    }

    /**
     * Read every part of the project. Textures come first, so a face can be checked against
     * them as it is read.
     *
     * @return the model data
     */
    private ModelData read() {
        JsonObject meta = this.json.get("meta") instanceof JsonObject object ? object : new JsonObject();
        String format = JsonFields.string(meta, "model_format", "free");
        if (!KNOWN_FORMATS.contains(format)) {
            this.report.warn("project", String.format(UNKNOWN_FORMAT, format));
        }

        JsonObject resolution = this.json.get("resolution") instanceof JsonObject object ? object : new JsonObject();
        int resolutionWidth = (int) JsonFields.number(resolution, "width", 16f);
        int resolutionHeight = (int) JsonFields.number(resolution, "height", 16f);

        for (JsonObject texture : JsonFields.objects(this.json, "textures")) {
            this.textures.add(this.texture(texture, resolutionWidth, resolutionHeight));
        }

        for (JsonObject element : JsonFields.objects(this.json, "elements")) {
            this.element(element);
        }

        for (JsonObject group : JsonFields.objects(this.json, "groups")) {
            this.groupFields.put(JsonFields.string(group, "uuid", ""), group);
        }

        List<String> roots = this.json.get("outliner") instanceof JsonArray outliner
                ? this.outliner(outliner)
                : List.of();
        List<Animation> animations = new ArrayList<>();
        for (JsonObject animation : JsonFields.objects(this.json, "animations")) {
            animations.add(this.animation(animation));
        }

        return new ModelData(
                JsonFields.string(this.json, "name", ""),
                format,
                resolutionWidth,
                resolutionHeight,
                this.groups,
                this.cubes,
                this.locators,
                roots,
                this.textures,
                animations,
                JsonMetadata.of(this.json, PROJECT_FIELDS)
        );
    }

    /**
     * Read one entry of the elements list into a cube or a locator.
     *
     * @param element the element's JSON
     */
    private void element(final JsonObject element) {
        if (!JsonFields.bool(element, "export", true)) {
            return;
        }

        String uuid = JsonFields.string(element, "uuid", UUID.randomUUID().toString());
        String name = JsonFields.string(element, "name", uuid);
        String type = JsonFields.string(element, "type", "cube");
        switch (type) {
            case "cube" -> this.cubes.put(uuid, this.cube(element, name));
            case "locator", "null_object" -> this.locators.put(uuid, new Locator(
                    uuid,
                    name,
                    JsonFields.vector(element, "position"),
                    JsonFields.vector(element, "rotation"),
                    JsonMetadata.of(element, LOCATOR_FIELDS)
            ));
            default -> this.report.warn(where("element", name), String.format(UNRENDERED_ELEMENT, type));
        }
    }

    /**
     * Read a cube and its faces.
     *
     * @param element the cube's JSON
     * @param name the cube's name
     * @return the cube
     */
    private Cube cube(final JsonObject element, final String name) {
        Map<FaceSide, CubeFace> faces = new EnumMap<>(FaceSide.class);
        if (element.get("faces") instanceof JsonObject faceObjects) {
            for (Map.Entry<String, JsonElement> entry : faceObjects.entrySet()) {
                Optional<FaceSide> side = FaceSide.fromKey(entry.getKey());
                if (side.isPresent() && entry.getValue() instanceof JsonObject face) {
                    faces.put(side.get(), this.face(face, name, side.get()));
                }
            }
        }

        return new Cube(
                name,
                JsonFields.vector(element, "from"),
                JsonFields.vector(element, "to"),
                JsonFields.vector(element, "origin"),
                JsonFields.vector(element, "rotation"),
                JsonFields.number(element, "inflate", 0f),
                faces,
                JsonFields.bool(element, "visibility", true)
        );
    }

    /**
     * Read one face of a cube. A face pointing past the last texture shows nothing.
     *
     * @param face the face's JSON
     * @param cube the cube's name, for warnings
     * @param side which face this is
     * @return the face
     */
    private CubeFace face(final JsonObject face, final String cube, final FaceSide side) {
        float[] uv = new float[4];
        if (face.get("uv") instanceof JsonArray array) {
            for (int i = 0; i < 4 && i < array.size(); i++) {
                uv[i] = JsonFields.number(array.get(i), 0f);
            }
        }

        int texture = CubeFace.NO_TEXTURE;
        if (face.get("texture") instanceof JsonPrimitive primitive && primitive.isNumber()) {
            texture = primitive.getAsInt();
        }

        if (texture >= this.textures.size() || texture < CubeFace.NO_TEXTURE) {
            String message = String.format(MISSING_TEXTURE, side.key(), texture, this.textures.size());
            this.report.warn(where("cube", cube), message);
            texture = CubeFace.NO_TEXTURE;
        }

        int rotation = (int) JsonFields.number(face, "rotation", 0f);

        return new CubeFace(uv[0], uv[1], uv[2], uv[3], texture, rotation);
    }

    /**
     * Read a list of outliner entries: ids of elements and group objects.
     *
     * @param entries the entries
     * @return the ids of the entries Hyperion kept, in order
     */
    private List<String> outliner(final JsonArray entries) {
        List<String> ids = new ArrayList<>(entries.size());
        for (JsonElement entry : entries) {
            if (entry instanceof JsonPrimitive primitive && primitive.isString()) {
                String id = primitive.getAsString();
                if (this.cubes.containsKey(id) || this.locators.containsKey(id)) {
                    ids.add(id);
                }

                continue;
            }

            if (entry instanceof JsonObject object) {
                String id = this.group(object);
                if (id != null) {
                    ids.add(id);
                }
            }
        }

        return ids;
    }

    /**
     * Read a group from the outliner, with its fields from the outliner entry itself or from the
     * top-level groups list.
     *
     * @param entry the outliner entry
     * @return the group's id, or null when the group is missing or not exported
     */
    private @Nullable String group(final JsonObject entry) {
        String uuid = JsonFields.string(entry, "uuid", UUID.randomUUID().toString());
        JsonObject fields = entry.has("name") ? entry : this.groupFields.get(uuid);
        if (fields == null) {
            this.report.warn("outliner", String.format(MISSING_GROUP, uuid));
            return null;
        }

        if (!JsonFields.bool(fields, "export", true)) {
            return null;
        }

        List<String> children = entry.get("children") instanceof JsonArray array ? this.outliner(array) : List.of();
        Metadata metadata = JsonMetadata.of(fields, GROUP_FIELDS);
        this.groups.put(uuid, new Group(
                uuid,
                JsonFields.string(fields, "name", uuid),
                JsonFields.vector(fields, "origin"),
                JsonFields.vector(fields, "rotation"),
                JsonFields.bool(fields, "visibility", true),
                children,
                metadata
        ));

        return uuid;
    }

    /**
     * Read a texture: decode the PNG saved in the project and work out its frames.
     *
     * @param texture the texture's JSON
     * @param resolutionWidth the project's UV width, used when the texture has none
     * @param resolutionHeight the project's UV height, used when the texture has none
     * @return the texture; one without a readable image has no bytes and a size of 0
     */
    private Texture texture(final JsonObject texture, final int resolutionWidth, final int resolutionHeight) {
        String name = JsonFields.string(texture, "name", "texture");
        String source = JsonFields.string(texture, "source", "");
        byte[] png = new byte[0];
        if (source.startsWith(PNG_PREFIX)) {
            try {
                png = Base64.getDecoder().decode(source.substring(PNG_PREFIX.length()));
            } catch (IllegalArgumentException exception) {
                this.report.warn(where("texture", name), BAD_IMAGE);
            }
        } else {
            this.report.warn(where("texture", name), NO_IMAGE);
        }

        int width = 0;
        int height = 0;
        if (png.length >= PNG_HEADER_LENGTH && ByteBuffer.wrap(png).getLong() == PNG_SIGNATURE) {
            width = ByteBuffer.wrap(png).getInt(PNG_WIDTH_OFFSET);
            height = ByteBuffer.wrap(png).getInt(PNG_WIDTH_OFFSET + 4);
        } else if (png.length > 0) {
            this.report.warn(where("texture", name), BAD_IMAGE);
            png = new byte[0];
        }

        int uvWidth = (int) JsonFields.number(texture, "uv_width", resolutionWidth);
        int uvHeight = (int) JsonFields.number(texture, "uv_height", resolutionHeight);
        int frames = Texture.frames(width, height, uvWidth, uvHeight);

        return new Texture(
                name,
                width,
                height,
                uvWidth,
                uvHeight,
                png,
                Math.max(1, (int) JsonFields.number(texture, "frame_time", 1f)),
                JsonFields.bool(texture, "frame_interpolate", false),
                frameOrder(texture, frames)
        );
    }

    /**
     * Work out the order an animated texture plays its frames in.
     *
     * @param texture the texture's JSON
     * @param frames how many frames the image holds
     * @return the frame indices, empty to play them top to bottom
     */
    private static List<Integer> frameOrder(final JsonObject texture, final int frames) {
        List<Integer> order = new ArrayList<>();
        switch (JsonFields.string(texture, "frame_order_type", "loop")) {
            case "custom" -> {
                String typed = JsonFields.string(texture, "frame_order", "").trim();
                for (String frame : WHITESPACE.splitAsStream(typed).toList()) {
                    try {
                        order.add(Integer.parseInt(frame));
                    } catch (NumberFormatException exception) {
                        // Blockbench leaves the field empty when no custom order was typed.
                    }
                }
            }
            case "backwards" -> {
                for (int frame = frames - 1; frame >= 0; frame--) {
                    order.add(frame);
                }
            }
            case "back_and_forth" -> {
                for (int frame = 0; frame < frames; frame++) {
                    order.add(frame);
                }

                for (int frame = frames - 2; frame > 0; frame--) {
                    order.add(frame);
                }
            }
            default -> {
                // Plain looping plays the frames top to bottom, which an empty order means.
            }
        }

        return order;
    }

    /**
     * Read an animation and its animators.
     *
     * @param animation the animation's JSON
     * @return the animation
     */
    private Animation animation(final JsonObject animation) {
        String name = JsonFields.string(animation, "name", "animation");
        String loopKey = JsonFields.string(animation, "loop", "once");
        LoopMode loop = LoopMode.fromKey(loopKey).orElse(null);
        if (loop == null) {
            this.report.warn(where("animation", name), String.format(UNKNOWN_LOOP, loopKey));
            loop = LoopMode.ONCE;
        }

        List<Animator> animators = new ArrayList<>();
        if (animation.get("animators") instanceof JsonObject animatorObjects) {
            for (Map.Entry<String, JsonElement> entry : animatorObjects.entrySet()) {
                if (entry.getValue() instanceof JsonObject animator) {
                    this.animator(name, entry.getKey(), animator).ifPresent(animators::add);
                }
            }
        }

        return new Animation(
                name,
                loop,
                JsonFields.number(animation, "length", 0f),
                JsonFields.bool(animation, "override", false),
                delay(animation, "start_delay"),
                delay(animation, "loop_delay"),
                animators
        );
    }

    /**
     * Read one animator of an animation.
     *
     * @param animation the animation's name, for warnings
     * @param target the id of what it drives
     * @param animator the animator's JSON
     * @return the animator, or empty for one that drives something Hyperion does not animate
     */
    private Optional<Animator> animator(final String animation, final String target, final JsonObject animator) {
        String name = JsonFields.string(animator, "name", target);
        String typeKey = JsonFields.string(animator, "type", EFFECTS_TARGET.equals(target) ? "effect" : "bone");
        AnimatorType type = switch (typeKey) {
            case "bone" -> AnimatorType.BONE;
            case "effect" -> AnimatorType.EFFECT;
            default -> null;
        };

        if (type == null) {
            this.report.warn(where("animation", animation), String.format(UNKNOWN_ANIMATOR, typeKey, name));
            return Optional.empty();
        }

        List<Keyframe> keyframes = new ArrayList<>();
        for (JsonObject keyframe : JsonFields.objects(animator, "keyframes")) {
            this.keyframe(animation, name, keyframe).ifPresent(keyframes::add);
        }

        keyframes.sort(Comparator.comparingDouble(Keyframe::time));

        return Optional.of(new Animator(target, name, type, keyframes));
    }

    /**
     * Read one keyframe, keeping every value as text.
     *
     * @param animation the animation's name, for warnings
     * @param animator the animator's name, for warnings
     * @param keyframe the keyframe's JSON
     * @return the keyframe, or empty for a channel Hyperion does not know
     */
    private Optional<Keyframe> keyframe(final String animation, final String animator, final JsonObject keyframe) {
        String channelKey = JsonFields.string(keyframe, "channel", "");
        KeyframeChannel channel = KeyframeChannel.fromKey(channelKey).orElse(null);
        if (channel == null) {
            this.report.warn(where("animation", animation), String.format(UNKNOWN_CHANNEL, channelKey, animator));
            return Optional.empty();
        }

        String interpolationKey = JsonFields.string(keyframe, "interpolation", "linear");
        Interpolation interpolation = Interpolation.fromKey(interpolationKey).orElse(null);
        if (interpolation == null) {
            String message = String.format(UNKNOWN_INTERPOLATION, interpolationKey, animator);
            this.report.warn(where("animation", animation), message);
            interpolation = Interpolation.LINEAR;
        }

        List<KeyframePoint> points = new ArrayList<>();
        for (JsonObject point : JsonFields.objects(keyframe, "data_points")) {
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> field : point.entrySet()) {
                if (field.getValue() instanceof JsonPrimitive primitive) {
                    values.put(field.getKey(), primitive.getAsString());
                }
            }

            points.add(new KeyframePoint(values));
        }

        Bezier bezier = interpolation != Interpolation.BEZIER ? null : new Bezier(
                JsonFields.vector(keyframe, "bezier_left_time"),
                JsonFields.vector(keyframe, "bezier_left_value"),
                JsonFields.vector(keyframe, "bezier_right_time"),
                JsonFields.vector(keyframe, "bezier_right_value")
        );

        float time = JsonFields.number(keyframe, "time", 0f);

        return Optional.of(new Keyframe(channel, time, interpolation, points, bezier));
    }

    /**
     * Read a delay field, which Blockbench saves as a number, Molang or an empty text.
     *
     * @param animation the animation's JSON
     * @param field the field name
     * @return the delay as text, {@code 0} when empty
     */
    private static String delay(final JsonObject animation, final String field) {
        String delay = JsonFields.string(animation, field, "").trim();

        return delay.isEmpty() ? "0" : delay;
    }

    /**
     * Name a part of the model for a warning.
     *
     * @param kind what the part is, such as {@code cube}
     * @param name the part's name
     * @return the label, such as {@code cube 'jaw'}
     */
    private static String where(final String kind, final String name) {
        return kind + " '" + name + "'";
    }
}
