package dev.vertcode.hyperion.bedrock;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.pack.PackWriter;

/**
 * Build the Bedrock resource pack, and the model list the Geyser extension registers.
 *
 * <p>Each model becomes a Bedrock entity {@code hyperion:<slug>}: its geometry, its textures in one
 * atlas, its animations, and one animation controller per animation that plays it while the
 * boolean property {@code hyperion:a<number>} is true. The server sets those properties through
 * the extension, so animations run on the player's device. A Bedrock entity holds at most 32
 * properties, so a model brings its first 30 animations and the speed.
 */
public final class BedrockPackBuilder {

    /**
     * The most animations a model brings to Bedrock.
     */
    public static final int MAX_ANIMATIONS = 30;

    private static final String RENDER_CONTROLLER = "controller.render.hyperion";
    private static final UUID HEADER = UUID.nameUUIDFromBytes("hyperion:bedrock-pack".getBytes(StandardCharsets.UTF_8));
    private static final UUID MODULE = UUID.nameUUIDFromBytes("hyperion:bedrock-module".getBytes(StandardCharsets.UTF_8));

    private BedrockPackBuilder() {
    }

    /**
     * Build the pack for some blueprints.
     *
     * @param blueprints the blueprints
     * @return the pack and the model list
     */
    public static BedrockPack build(final List<Blueprint> blueprints) {
        PackWriter writer = new PackWriter();
        writer.json("render_controllers/hyperion.render_controllers.json", renderController());
        JsonArray models = new JsonArray();
        for (Blueprint blueprint : blueprints) {
            models.add(model(writer, blueprint));
        }

        // The version follows the contents, so clients that cached an older pack fetch this one.
        String contents = PackWriter.sha1(writer.zip());
        writer.json("manifest.json", manifest(contents));
        byte[] zip = writer.zip();
        String sha1 = PackWriter.sha1(zip);
        JsonObject manifest = new JsonObject();
        manifest.addProperty("version", sha1);
        manifest.add("models", models);

        return new BedrockPack(zip, sha1, manifest.toString());
    }

    /**
     * Write one model's files and describe it for the model list.
     *
     * @param writer the pack being written
     * @param blueprint the blueprint
     * @return the model's entry in the list
     */
    private static JsonObject model(final PackWriter writer, final Blueprint blueprint) {
        String slug = BedrockIds.slug(blueprint.key());
        List<String> names = new ArrayList<>(blueprint.animations());
        if (names.size() > MAX_ANIMATIONS) {
            names = names.subList(0, MAX_ANIMATIONS);
        }

        BedrockAtlas atlas = BedrockAtlas.of(blueprint.textures());
        writer.bytes("textures/hyperion/" + slug + ".png", atlas.png());
        writer.json("models/entity/" + slug + ".geo.json", BedrockGeometry.of(blueprint, slug, atlas));
        writer.json("animations/" + slug + ".animation.json", BedrockAnimations.of(blueprint, slug, names));
        writer.json("animation_controllers/" + slug + ".animation_controllers.json", controllers(slug, names.size()));
        writer.json("entity/" + slug + ".entity.json", entity(slug, names.size()));

        JsonObject entry = new JsonObject();
        entry.addProperty("id", BedrockIds.id(blueprint.key()));
        entry.addProperty("key", blueprint.key().asString());
        entry.addProperty("identifier", BedrockIds.identifier(blueprint.key()));
        JsonArray animations = new JsonArray();
        names.forEach(animations::add);
        entry.add("animations", animations);

        return entry;
    }

    /**
     * Return the property that plays one animation.
     *
     * @param index the animation's number
     * @return the property's identifier
     */
    public static String property(final int index) {
        return "hyperion:a" + index;
    }

    private static JsonObject controllers(final String slug, final int count) {
        JsonObject controllers = new JsonObject();
        for (int i = 0; i < count; i++) {
            String playing = "query.property('" + property(i) + "')";
            JsonObject off = new JsonObject();
            off.add("transitions", transition("on", playing));
            JsonObject on = new JsonObject();
            JsonArray animations = new JsonArray();
            animations.add("a" + i);
            on.add("animations", animations);
            on.add("transitions", transition("off", "!" + playing));
            on.addProperty("blend_transition", 0.15f);
            JsonObject states = new JsonObject();
            states.add("off", off);
            states.add("on", on);
            JsonObject controller = new JsonObject();
            controller.addProperty("initial_state", "off");
            controller.add("states", states);
            controllers.add(controller(slug, i), controller);
        }

        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.10.0");
        file.add("animation_controllers", controllers);

        return file;
    }

    private static JsonObject entity(final String slug, final int count) {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "hyperion:" + slug);
        JsonObject materials = new JsonObject();
        materials.addProperty("default", "entity_alphatest");
        description.add("materials", materials);
        JsonObject textures = new JsonObject();
        textures.addProperty("default", "textures/hyperion/" + slug);
        description.add("textures", textures);
        JsonObject geometry = new JsonObject();
        geometry.addProperty("default", "geometry.hyperion." + slug);
        description.add("geometry", geometry);
        JsonArray renderControllers = new JsonArray();
        renderControllers.add(RENDER_CONTROLLER);
        description.add("render_controllers", renderControllers);

        JsonObject animations = new JsonObject();
        JsonArray animate = new JsonArray();
        for (int i = 0; i < count; i++) {
            animations.addProperty("a" + i, BedrockAnimations.name(slug, i));
            animations.addProperty("c" + i, controller(slug, i));
            animate.add("c" + i);
        }

        description.add("animations", animations);
        JsonObject scripts = new JsonObject();
        scripts.add("animate", animate);
        description.add("scripts", scripts);

        JsonObject entity = new JsonObject();
        entity.add("description", description);
        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.10.0");
        file.add("minecraft:client_entity", entity);

        return file;
    }

    private static String controller(final String slug, final int index) {
        return "controller.animation.hyperion." + slug + "." + index;
    }

    private static JsonArray transition(final String state, final String condition) {
        JsonObject transition = new JsonObject();
        transition.addProperty(state, condition);
        JsonArray transitions = new JsonArray();
        transitions.add(transition);

        return transitions;
    }

    private static JsonObject renderController() {
        JsonObject materials = new JsonObject();
        materials.addProperty("*", "Material.default");
        JsonArray materialList = new JsonArray();
        materialList.add(materials);
        JsonArray textures = new JsonArray();
        textures.add("Texture.default");
        JsonObject controller = new JsonObject();
        controller.addProperty("geometry", "Geometry.default");
        controller.add("materials", materialList);
        controller.add("textures", textures);
        JsonObject controllers = new JsonObject();
        controllers.add(RENDER_CONTROLLER, controller);
        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.8.0");
        file.add("render_controllers", controllers);

        return file;
    }

    /**
     * Write the pack's manifest, versioned by its contents.
     *
     * @param contents the hash of every other file
     * @return the manifest
     */
    private static JsonObject manifest(final String contents) {
        int hash = contents.hashCode();
        JsonArray version = new JsonArray();
        version.add(1);
        version.add((hash >>> 16) & 0xFFFF);
        version.add(hash & 0xFFFF);
        JsonArray engine = new JsonArray();
        engine.add(1);
        engine.add(21);
        engine.add(0);

        JsonObject header = new JsonObject();
        header.addProperty("name", "Hyperion");
        header.addProperty("description", "The models of this server");
        header.addProperty("uuid", HEADER.toString());
        header.add("version", version);
        header.add("min_engine_version", engine);
        JsonObject module = new JsonObject();
        module.addProperty("type", "resources");
        module.addProperty("uuid", MODULE.toString());
        module.add("version", version.deepCopy());
        JsonArray modules = new JsonArray();
        modules.add(module);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("format_version", 2);
        manifest.add("header", header);
        manifest.add("modules", modules);

        return manifest;
    }
}
