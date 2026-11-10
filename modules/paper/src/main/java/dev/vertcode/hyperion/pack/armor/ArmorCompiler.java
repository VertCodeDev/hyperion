package dev.vertcode.hyperion.pack.armor;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.function.IntSupplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.version.Feature;
import dev.vertcode.hyperion.api.version.MinecraftVersion;
import dev.vertcode.hyperion.armor.ArmorBone;
import dev.vertcode.hyperion.armor.ArmorItems;
import dev.vertcode.hyperion.armor.ArmorLayer;
import dev.vertcode.hyperion.armor.ArmorMaterial;
import dev.vertcode.hyperion.armor.ArmorPackInfo;
import dev.vertcode.hyperion.armor.HyperionArmorSet;
import dev.vertcode.hyperion.pack.ModelOverride;
import dev.vertcode.hyperion.pack.PackWriter;

/**
 * Write every armor set into a pack, the way the server's version draws armor.
 *
 * <ul>
 *   <li>Every version: the helmet's model, worn as an item, and each piece's icon.</li>
 *   <li>From 1.21.2: an equipment asset per set. Its first layer is the flat look; with the
 *       shader on, more layers carry the 3D faces, invisible to any client without the shader.
 *       The patched entity shader goes into an overlay only the server's own version reads.</li>
 *   <li>1.21 to 1.21.1: the leather texture widened with a cell per set, and the patched armor
 *       shader that picks the cell, in the same overlay.</li>
 * </ul>
 */
public final class ArmorCompiler {

    private static final String SHADERS = "assets/minecraft/shaders/";
    private static final String ENTITY_VERTEX = SHADERS + "core/entity.vsh";
    private static final String ENTITY_FRAGMENT = SHADERS + "core/entity.fsh";
    private static final String ARMOR_VERTEX = SHADERS + "core/rendertype_armor_cutout_no_cull.vsh";
    private static final String LEATHER = "assets/minecraft/textures/models/armor/leather_layer_";
    private static final int MAX_SET_NUMBER = 254;
    private static final String DROPPED = "%s: %d face(s) on the %s texture did not fit in %d layers; raise "
            + "armor.max-layers or use fewer faces";
    private static final String TOO_MANY = "Only %d armor sets can be told apart on 1.21 to 1.21.1; the rest show as leather";
    private static final String NO_PATCH = "The vanilla %s of this version has changed in a way Hyperion does not know, "
            + "so custom armor is drawn flat";
    private static final String NO_CLIENT_FLAT = " (on 1.21 to 1.21.1, as plain leather)";

    private final ArmorBuild build;
    private final MinecraftVersion version;
    private final PackWriter writer;
    private final IntSupplier customModelData;
    private final List<ModelOverride> baseOverrides;
    private final Map<String, List<ModelOverride>> itemOverrides = new TreeMap<>();
    private final Map<Key, Map<ArmorSlot, Integer>> numbers = new LinkedHashMap<>();
    private @Nullable ClientJar client;
    private boolean clientOpened;
    private boolean overlay;

    /**
     * Prepare to write armor.
     *
     * @param build the sets and settings
     * @param version the version the pack is for
     * @param writer the pack being written
     * @param customModelData hands out the next custom model data number, before 1.21.4
     * @param baseOverrides the base item's overrides, where 1.21 to 1.21.1 helmets go
     */
    public ArmorCompiler(
            final ArmorBuild build,
            final MinecraftVersion version,
            final PackWriter writer,
            final IntSupplier customModelData,
            final List<ModelOverride> baseOverrides
    ) {
        this.build = build;
        this.version = version;
        this.writer = writer;
        this.customModelData = customModelData;
        this.baseOverrides = baseOverrides;
    }

    /**
     * Write every set.
     *
     * @return the numbers armor items need, and whether the overlay was used
     */
    public ArmorPackResult compile() {
        List<HyperionArmorSet> sets = this.build.sets();
        if (sets.isEmpty()) {
            return ArmorPackResult.empty();
        }

        boolean equippable = Feature.EQUIPPABLE.availableOn(this.version);
        boolean geometry = false;
        for (HyperionArmorSet set : sets) {
            this.items(set);
            if (equippable) {
                geometry |= this.equipment(set);
            }
        }

        this.itemOverrides.forEach((item, overrides) -> this.writer.json(
                "assets/minecraft/models/item/" + item + ".json",
                TrimOverrides.model(item, darker(item), overrides)
        ));

        Map<Key, Integer> setNumbers = Map.of();
        if (equippable) {
            if (geometry) {
                this.entityShader();
            }
        } else {
            setNumbers = this.leather(sets);
        }

        return new ArmorPackResult(new ArmorPackInfo(this.numbers, setNumbers), this.overlay);
    }

    /**
     * Write a set's item models, and its item definitions or custom model data.
     *
     * @param set the set
     */
    private void items(final HyperionArmorSet set) {
        ArmorItemModels models = new ArmorItemModels(set, this.writer);
        boolean definitions = Feature.ITEM_MODEL_DEFINITIONS.availableOn(this.version);
        boolean equippable = Feature.EQUIPPABLE.availableOn(this.version);
        for (ArmorSlot slot : ArmorSlot.values()) {
            List<ArmorBone> bones = set.bones(slot);
            if (bones.isEmpty()) {
                continue;
            }

            OptionalInt iconTexture = set.textures().icon(slot);
            String worn = null;
            String icon;
            if (slot == ArmorSlot.HEAD) {
                worn = models.helmet();
                icon = iconTexture.isPresent() && definitions ? models.icon(slot.piece(), bones, iconTexture.getAsInt()) : worn;
            } else {
                icon = models.icon(slot.piece(), bones, iconTexture.orElse(-1));
            }

            if (definitions) {
                String path = ArmorItems.itemModel(set.key(), slot).replace(':', '/');
                int colon = path.indexOf('/');
                this.writer.json(
                        "assets/" + path.substring(0, colon) + "/items/" + path.substring(colon + 1) + ".json",
                        definition(worn, icon, set.dyeable())
                );
                continue;
            }

            int number = this.customModelData.getAsInt();
            this.numbers.computeIfAbsent(set.key(), key -> new EnumMap<>(ArmorSlot.class)).put(slot, number);
            ModelOverride override = new ModelOverride(number, icon);
            if (slot == ArmorSlot.HEAD && !equippable) {
                this.baseOverrides.add(override);
            } else {
                String item = equippable ? set.material().item(slot) : ArmorMaterial.LEATHER.item(slot);
                this.itemOverrides.computeIfAbsent(item, key -> new ArrayList<>()).add(override);
            }
        }
    }

    /**
     * Write a set's equipment asset and its layers' textures.
     *
     * @param set the set
     * @return whether the set has 3D layers
     */
    private boolean equipment(final HyperionArmorSet set) {
        JsonObject layers = new JsonObject();
        boolean geometry = false;
        for (ArmorLayer layer : ArmorLayer.values()) {
            BufferedImage flat = FlatTextures.base(set, layer);
            if (flat == null) {
                continue;
            }

            FlatTextures.glow(set, layer, flat);
            BufferedImage tint = FlatTextures.tint(set, layer);
            GeometryLayers shapes = this.build.settings().shader()
                    ? GeometryLayers.encode(set, layer, this.build.settings().maxLayers())
                    : new GeometryLayers(List.of(), 0, 0);
            if (shapes.dropped() > 0) {
                this.build.logger().warning(String.format(
                        DROPPED, set.key().asString(), shapes.dropped(), layer.asset(), this.build.settings().maxLayers()));
            }

            if (shapes.parts() != 0) {
                geometry = true;
                ArmorShaderData.header(flat, ArmorShaderData.KIND_FLAT, shapes.parts(), layer.inflate());
                if (tint != null) {
                    ArmorShaderData.header(tint, ArmorShaderData.KIND_FLAT, shapes.parts(), layer.inflate());
                }
            }

            JsonArray entries = new JsonArray();
            entries.add(this.layer(set, layer, "", flat, false));
            if (tint != null) {
                entries.add(this.layer(set, layer, "_tint", tint, set.dyeable()));
            }

            List<BufferedImage> images = shapes.images();
            for (int index = 0; index < images.size(); index++) {
                entries.add(this.layer(set, layer, "_3d_" + (index + 1), images.get(index), set.dyeable()));
            }

            layers.add(layer.asset(), entries);
        }

        JsonObject asset = new JsonObject();
        asset.add("layers", layers);
        String folder = Feature.ITEM_MODEL_DEFINITIONS.availableOn(this.version) ? "/equipment/" : "/models/equipment/";
        this.writer.json("assets/" + set.key().namespace() + folder + set.id() + ".json", asset);

        return geometry;
    }

    /**
     * Write one layer's texture and describe the layer.
     *
     * @param set the set
     * @param layer the vanilla texture it is drawn on
     * @param suffix what tells this layer's texture from the set's others
     * @param image the texture
     * @param dyeable whether the layer takes the dye color
     * @return the layer's entry in the equipment asset
     */
    private JsonObject layer(
            final HyperionArmorSet set,
            final ArmorLayer layer,
            final String suffix,
            final BufferedImage image,
            final boolean dyeable
    ) {
        String name = set.id() + suffix;
        String path = "assets/" + set.key().namespace() + "/textures/entity/equipment/" + layer.asset() + "/" + name + ".png";
        this.writer.bytes(path, ArmorImages.png(image));
        JsonObject entry = new JsonObject();
        entry.addProperty("texture", set.key().namespace() + ":" + name);
        if (dyeable) {
            JsonObject dye = new JsonObject();
            dye.addProperty("color_when_undyed", -1);
            entry.add("dyeable", dye);
        }

        return entry;
    }

    /**
     * Patch the entity shaders for 3D armor, into the overlay.
     */
    private void entityShader() {
        Optional<ClientJar> jar = this.client("");
        if (jar.isEmpty()) {
            return;
        }

        String vertex = jar.get().text(ENTITY_VERTEX).orElse("");
        String fragment = jar.get().text(ENTITY_FRAGMENT).orElse("");
        Optional<String> patchedVertex = ShaderPatcher.armorVertex(vertex, this.build.settings().renderDistance());
        Optional<String> patchedFragment = ShaderPatcher.armorFragment(fragment, vertex);
        if (patchedVertex.isEmpty() || patchedFragment.isEmpty()) {
            this.build.logger().warning(String.format(NO_PATCH, "entity shader"));
            return;
        }

        this.overlayText(ENTITY_VERTEX, patchedVertex.get());
        this.overlayText(ENTITY_FRAGMENT, patchedFragment.get());
    }

    /**
     * Widen the leather textures with a cell per set and patch the armor shader to pick it, for
     * 1.21 to 1.21.1.
     *
     * @param sets the sets
     * @return the number of each set that has leather pieces
     */
    private Map<Key, Integer> leather(final List<HyperionArmorSet> sets) {
        List<HyperionArmorSet> leathered = new ArrayList<>();
        for (HyperionArmorSet set : sets) {
            boolean worn = !set.bones(ArmorSlot.CHEST).isEmpty()
                    || !set.bones(ArmorSlot.LEGS).isEmpty()
                    || !set.bones(ArmorSlot.FEET).isEmpty();
            if (worn) {
                leathered.add(set);
            }
        }

        if (leathered.isEmpty()) {
            return Map.of();
        }

        if (leathered.size() > MAX_SET_NUMBER) {
            this.build.logger().warning(String.format(TOO_MANY, MAX_SET_NUMBER));
            leathered = leathered.subList(0, MAX_SET_NUMBER);
        }

        Optional<ClientJar> jar = this.client(NO_CLIENT_FLAT);
        if (jar.isEmpty()) {
            return Map.of();
        }

        Optional<String> patched = jar.get().text(ARMOR_VERTEX).flatMap(ShaderPatcher::leatherVertex);
        if (patched.isEmpty()) {
            this.build.logger().warning(String.format(NO_PATCH, "armor shader"));
            return Map.of();
        }

        this.overlayText(ARMOR_VERTEX, patched.get());
        for (int layer = 1; layer <= 2; layer++) {
            ArmorLayer armorLayer = layer == 1 ? ArmorLayer.OUTER : ArmorLayer.INNER;
            BufferedImage vanilla = jar.get().read(LEATHER + layer + ".png")
                    .map(ArmorImages::read)
                    .orElseGet(() -> ArmorImages.blank(FlatTextures.LAYOUT_WIDTH, FlatTextures.LAYOUT_HEIGHT));
            this.overlayBytes(LEATHER + layer + ".png", ArmorImages.png(LeatherAtlas.build(vanilla, leathered, armorLayer)));

            // The overlay pass is drawn undyed, so it cannot tell sets apart; it is left empty.
            this.overlayBytes(LEATHER + layer + "_overlay.png", ArmorImages.png(ArmorImages.blank(
                    FlatTextures.LAYOUT_WIDTH, FlatTextures.LAYOUT_HEIGHT)));
        }

        Map<Key, Integer> numbers = new LinkedHashMap<>();
        for (int index = 0; index < leathered.size(); index++) {
            numbers.put(leathered.get(index).key(), index + 1);
        }

        return numbers;
    }

    /**
     * Open the client jar once per build.
     *
     * @param fallback what the log says custom armor falls back to
     * @return the jar, or empty when it could not be had
     */
    private Optional<ClientJar> client(final String fallback) {
        if (!this.clientOpened) {
            this.clientOpened = true;
            this.client = ClientJar.open(this.build.cache(), this.build.minecraftVersion(), this.build.logger(), fallback)
                    .orElse(null);
        }

        return Optional.ofNullable(this.client);
    }

    /**
     * Write a text file into the overlay.
     *
     * @param path the file's path, as it would be in the pack itself
     * @param text the contents
     */
    private void overlayText(final String path, final String text) {
        this.overlayBytes(path, text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Write a file into the overlay.
     *
     * @param path the file's path, as it would be in the pack itself
     * @param bytes the contents
     */
    private void overlayBytes(final String path, final byte[] bytes) {
        this.writer.bytes(ArmorPackResult.OVERLAY + "/" + path, bytes);
        this.overlay = true;
    }

    /**
     * Write an item definition: the worn model on the head, and the icon everywhere else.
     *
     * @param worn the helmet's worn model, or null for a piece that is not worn as an item
     * @param icon the icon model
     * @param dyeable whether the models take the dye color
     * @return the definition
     */
    private static JsonObject definition(final @Nullable String worn, final String icon, final boolean dyeable) {
        JsonObject model;
        if (worn == null || worn.equals(icon)) {
            model = drawn(worn == null ? icon : worn, dyeable);
        } else {
            JsonObject head = new JsonObject();
            head.addProperty("when", "head");
            head.add("model", drawn(worn, dyeable));
            JsonArray cases = new JsonArray();
            cases.add(head);
            model = new JsonObject();
            model.addProperty("type", "minecraft:select");
            model.addProperty("property", "minecraft:display_context");
            model.add("cases", cases);
            model.add("fallback", drawn(icon, dyeable));
        }

        JsonObject definition = new JsonObject();
        definition.add("model", model);

        return definition;
    }

    /**
     * Write a plain model reference, tinted by the dye color when dyeable.
     *
     * @param model the model's id
     * @param dyeable whether it takes the dye color
     * @return the reference
     */
    private static JsonObject drawn(final String model, final boolean dyeable) {
        JsonObject drawn = new JsonObject();
        drawn.addProperty("type", "minecraft:model");
        drawn.addProperty("model", model);
        if (dyeable) {
            JsonObject tint = new JsonObject();
            tint.addProperty("type", "minecraft:dye");
            tint.addProperty("default", -1);
            JsonArray tints = new JsonArray();
            tints.add(tint);
            drawn.add("tints", tints);
        }

        return drawn;
    }

    /**
     * Return the trim material with a darker look on an armor item.
     *
     * @param item the item's name
     * @return the trim material, or null
     */
    private static @Nullable String darker(final String item) {
        for (ArmorMaterial material : ArmorMaterial.values()) {
            for (ArmorSlot slot : ArmorSlot.values()) {
                if (material.item(slot).equals(item)) {
                    return material.darkerTrim();
                }
            }
        }

        return null;
    }
}
