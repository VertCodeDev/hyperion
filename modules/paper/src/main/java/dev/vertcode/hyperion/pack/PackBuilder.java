package dev.vertcode.hyperion.pack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.version.Feature;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.blueprint.Blueprint;
import dev.vertcode.hyperion.config.PackSettings;
import dev.vertcode.hyperion.pack.armor.ArmorBuild;
import dev.vertcode.hyperion.pack.armor.ArmorCompiler;
import dev.vertcode.hyperion.pack.armor.ArmorPackResult;

/**
 * Build the resource pack for a set of blueprints and a Minecraft version.
 *
 * <p>Work on immutable input, so it runs on the async scheduler; only armor may reach out, to
 * download the client jar once. From 1.21.4 each part is an item definition picked with the
 * {@code item_model} component; before that, each part is a custom model data number on the
 * configured base item.
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
        return build(blueprints, settings, version, format, null);
    }

    /**
     * Build the pack, with armor sets.
     *
     * @param blueprints the blueprints to put in it
     * @param settings the pack settings, for the base item and the first custom model data number
     * @param version the version the pack is for
     * @param format the pack format that version reads
     * @param armor the armor sets and settings, or null for none
     * @return the built pack
     */
    public static ResourcePack build(
            final Collection<Blueprint> blueprints,
            final PackSettings settings,
            final MinecraftVersion version,
            final PackFormat format,
            final @Nullable ArmorBuild armor
    ) {
        long start = System.nanoTime();
        PackWriter writer = new PackWriter();

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

        ArmorPackResult armorResult = armor == null
                ? ArmorPackResult.empty()
                : new ArmorCompiler(armor, version, writer, customModelData::getAndIncrement, overrides).compile();
        writer.json("pack.mcmeta", meta(version, format, armorResult.overlay()));
        if (!overrides.isEmpty()) {
            String item = settings.baseItem();
            writer.json("assets/minecraft/models/item/" + item + ".json", baseItemModel(item, overrides));
        }

        byte[] zip = writer.zip();
        long millis = (System.nanoTime() - start) / 1_000_000L;

        return new ResourcePack(zip, PackWriter.sha1(zip), writer.size(), millis, parts, armorResult.info());
    }

    /**
     * Write {@code pack.mcmeta}. From 1.21.9 it also states the formats as a range. Armor's
     * patched shaders sit in an overlay only the server's own format reads, so a client of another
     * version, through ViaVersion, never loads a shader made for a different Minecraft.
     *
     * @param version the version the pack is for
     * @param format the pack format
     * @param overlay whether the pack has the armor overlay
     * @return the file's contents
     */
    private static JsonObject meta(final MinecraftVersion version, final PackFormat format, final boolean overlay) {
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
        if (overlay) {
            meta.add("overlays", overlays(version, format));
        }

        return meta;
    }

    /**
     * Write the overlay section: the armor overlay, for exactly the server's format.
     *
     * @param version the version the pack is for
     * @param format the pack format
     * @return the section
     */
    private static JsonObject overlays(final MinecraftVersion version, final PackFormat format) {
        JsonObject entry = new JsonObject();
        if (Feature.PACK_FORMAT_RANGE.availableOn(version)) {
            JsonArray range = new JsonArray();
            range.add(format.major());
            range.add(format.minor());
            entry.add("min_format", range);
            entry.add("max_format", range.deepCopy());
        } else {
            JsonArray formats = new JsonArray();
            formats.add(format.major());
            formats.add(format.major());
            entry.add("formats", formats);
        }

        entry.addProperty("directory", ArmorPackResult.OVERLAY);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject overlays = new JsonObject();
        overlays.add("entries", entries);

        return overlays;
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

}
