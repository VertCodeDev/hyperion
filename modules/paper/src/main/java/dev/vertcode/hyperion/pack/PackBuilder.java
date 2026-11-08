package dev.vertcode.hyperion.pack;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;

import dev.vertcode.hyperion.api.version.Feature;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.config.PackSettings;

/**
 * Build the resource pack for a set of blueprints and a Minecraft version.
 *
 * <p>Pure work on immutable input, so it runs on the async scheduler. From 1.21.4 each part is an
 * item definition picked with the {@code item_model} component; before that, each part is a
 * custom model data number on the configured base item.
 */
public final class PackBuilder {

    private static final String DESCRIPTION = "Models by Hyperion";

    private PackBuilder() {
    }

    /**
     * Build the pack.
     *
     * @param blueprints the blueprints to put in it
     * @param settings the pack settings, for the base item and the first custom model data number
     * @param version the version the pack is for
     * @param format the pack format that version reads
     * @return the built pack
     */
    public static ResourcePack build(
            final Collection<Blueprint> blueprints,
            final PackSettings settings,
            final MinecraftVersion version,
            final PackFormat format
    ) {
        long start = System.nanoTime();
        PackWriter writer = new PackWriter();
        writer.json("pack.mcmeta", meta(version, format));

        boolean itemDefinitions = Feature.ITEM_MODEL_DEFINITIONS.availableOn(version);
        AtomicInteger customModelData = new AtomicInteger(settings.customModelDataStart());
        List<ModelOverride> overrides = new ArrayList<>();
        Map<Key, Map<String, List<ModelPart>>> parts = new LinkedHashMap<>();
        List<Blueprint> sorted = new ArrayList<>(blueprints);
        sorted.sort(Comparator.comparing(blueprint -> blueprint.key().asString()));
        for (Blueprint blueprint : sorted) {
            BlueprintCompiler compiler = new BlueprintCompiler(
                    blueprint,
                    writer,
                    itemDefinitions,
                    customModelData::getAndIncrement,
                    overrides
            );
            parts.put(blueprint.key(), compiler.compile());
        }

        if (!overrides.isEmpty()) {
            String item = settings.baseItem();
            writer.json("assets/minecraft/models/item/" + item + ".json", baseItemModel(item, overrides));
        }

        byte[] zip = writer.zip();
        long millis = (System.nanoTime() - start) / 1_000_000L;

        return new ResourcePack(zip, sha1(zip), writer.size(), millis, parts);
    }

    /**
     * Write {@code pack.mcmeta}. From 1.21.9 it also states the formats as a range.
     *
     * @param version the version the pack is for
     * @param format the pack format
     * @return the file's contents
     */
    private static JsonObject meta(final MinecraftVersion version, final PackFormat format) {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", DESCRIPTION);
        pack.addProperty("pack_format", format.major());
        if (Feature.PACK_FORMAT_RANGE.availableOn(version)) {
            JsonArray range = new JsonArray();
            range.add(format.major());
            range.add(format.minor());
            pack.add("min_format", range);
            pack.add("max_format", range.deepCopy());
        }

        JsonObject meta = new JsonObject();
        meta.add("pack", pack);

        return meta;
    }

    /**
     * Write the base item's model for 1.21 to 1.21.3: the plain flat item, with an override per
     * part.
     *
     * @param item the base item's name, such as {@code paper}
     * @param overrides the custom model data numbers and their models
     * @return the model
     */
    private static JsonObject baseItemModel(final String item, final List<ModelOverride> overrides) {
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", "minecraft:item/" + item);

        List<ModelOverride> sorted = new ArrayList<>(overrides);
        sorted.sort(Comparator.comparingInt(ModelOverride::customModelData));
        JsonArray entries = new JsonArray();
        for (ModelOverride override : sorted) {
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", override.customModelData());
            JsonObject entry = new JsonObject();
            entry.add("predicate", predicate);
            entry.addProperty("model", override.model());
            entries.add(entry);
        }

        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        model.add("textures", textures);
        model.add("overrides", entries);

        return model;
    }

    /**
     * Hash the zip the way clients check it.
     *
     * @param zip the zip's bytes
     * @return the SHA-1 in lowercase hex
     */
    private static String sha1(final byte[] zip) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(zip));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Every Java runtime has SHA-1", exception);
        }
    }
}
