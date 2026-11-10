package dev.vertcode.hyperion.armor;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.armor.ArmorData;
import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.api.armor.ArmorStats;
import dev.vertcode.hyperion.blueprint.BlueprintKeys;
import dev.vertcode.hyperion.blueprint.IssueSeverity;
import dev.vertcode.hyperion.blueprint.LoadIssue;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.blueprint.bbmodel.BbmodelReader;
import dev.vertcode.hyperion.blueprint.data.Group;
import dev.vertcode.hyperion.blueprint.data.ModelData;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * Load the armor sets in {@code plugins/Hyperion/armor}: each {@code .bbmodel} is a set, with an
 * optional {@code .yml} of the same name for its stats and data.
 *
 * <pre>{@code
 * name: "<aqua>Frost"
 * base: netherite          # the vanilla armor the items are made from
 * armor: [3, 8, 6, 3]      # helmet, chestplate, leggings, boots; the base's when left out
 * toughness: 2
 * knockback-resistance: 0
 * dyeable: false
 * data:                    # free values for plugins
 *   element: ice
 * }</pre>
 *
 * <p>Reads files, so it runs on the async scheduler. A set that cannot load is left out with an
 * error in the log; the others still load.
 */
public final class ArmorLoader {

    /**
     * The folder sets are read from, in Hyperion's data folder.
     */
    public static final String FOLDER = "armor";

    private static final String MODEL = ".bbmodel";
    private static final Pattern WORDS = Pattern.compile("[_\\-. ]+");
    private static final String YAML = ".yml";
    private static final float PIVOT_TOLERANCE = 0.5f;
    private static final int SLOTS = 4;
    private static final int LAYER_ASPECT = 2;
    private static final String LOG = "%s: %s";
    private static final String UNREADABLE = "could not be read: %s";
    private static final String NOT_JSON = "is not a Blockbench file: %s";
    private static final String NO_BONES = "has no armor bones; name groups helmet, chestplate, right_arm, left_arm, "
            + "leggings, right_leg, left_leg, right_boot or left_boot";
    private static final String TWICE = "has more than one '%s' bone; only the first is used";
    private static final String PIVOT = "pivots at %s, but the %s pivots at %s; the bone's cubes stay where they are, "
            + "but they turn around the vanilla pivot";
    private static final String BAD_BASE = "base '%s' is no vanilla armor, so the set is made from netherite";
    private static final String BAD_ARMOR = "armor must be four numbers, helmet to boots, so the base's are used";
    private static final String FLAT_SIZE = "is %dx%d, but an armor texture is twice as wide as it is high, such as 64x32";
    private static final String NO_FOLDER = "Could not read the armor folder %s";

    private ArmorLoader() {
    }

    /**
     * Load every set in a folder and its subfolders.
     *
     * @param folder the folder
     * @param items builds the sets' items
     * @param logger where problems are logged
     * @return the sets that loaded, sorted by key
     */
    public static List<HyperionArmorSet> loadFolder(final Path folder, final ArmorItems items, final Logger logger) {
        List<HyperionArmorSet> sets = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return sets;
        }

        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(MODEL))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            logger.log(Level.WARNING, String.format(NO_FOLDER, folder), exception);
            return sets;
        }

        for (Path file : files) {
            String relative = folder.relativize(file).toString().replace('\\', '/');
            LoadReport report = new LoadReport(FOLDER + "/" + relative);
            load(file, relative, items, report).ifPresent(sets::add);
            log(report, logger);
        }

        sets.sort(Comparator.comparing(set -> set.key().asString()));

        return sets;
    }

    /**
     * Load one set from its file, and its {@code .yml} when there is one.
     *
     * @param file the {@code .bbmodel}
     * @param relative its path inside the armor folder
     * @param items builds the set's items
     * @param report where problems are noted
     * @return the set, or empty when it could not load
     */
    private static Optional<HyperionArmorSet> load(
            final Path file,
            final String relative,
            final ArmorItems items,
            final LoadReport report
    ) {
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            report.error(String.format(UNREADABLE, exception.getMessage()));
            return Optional.empty();
        } catch (JsonParseException | IllegalStateException exception) {
            report.error(String.format(NOT_JSON, exception.getMessage()));
            return Optional.empty();
        }

        String name = file.getFileName().toString();
        Path yaml = file.resolveSibling(name.substring(0, name.length() - MODEL.length()) + YAML);
        ConfigurationSection settings = Files.isRegularFile(yaml) ? YamlConfiguration.loadConfiguration(yaml.toFile()) : null;

        return read(relative, json, settings, items, report);
    }

    /**
     * Make a set from a parsed {@code .bbmodel} and its settings.
     *
     * @param relative the file's path inside the armor folder, which gives the key
     * @param json the parsed {@code .bbmodel}
     * @param settings the set's {@code .yml}, or null when it has none
     * @param items builds the set's items
     * @param report where problems are noted
     * @return the set, or empty when it could not load
     */
    public static Optional<HyperionArmorSet> read(
            final String relative,
            final JsonObject json,
            final @Nullable ConfigurationSection settings,
            final ArmorItems items,
            final LoadReport report
    ) {
        Optional<Key> key = BlueprintKeys.fromPath(relative, report);
        ModelData model = BbmodelReader.read(json, report);
        if (key.isEmpty() || report.hasErrors()) {
            return Optional.empty();
        }

        Map<ArmorBone, Group> bones = bones(model, report);
        if (bones.isEmpty()) {
            report.error(NO_BONES);
            return Optional.empty();
        }

        checkFlatTextures(model, report);
        ConfigurationSection section = settings == null ? new YamlConfiguration() : settings;
        ArmorMaterial material = material(section, report);
        Map<ArmorSlot, ArmorStats> stats = stats(section, material, report);
        String displayName = section.getString("name", title(key.get().value()));
        ArmorData data = new ArmorData(values(section.getConfigurationSection("data")));

        return Optional.of(new HyperionArmorSet(
                key.get(),
                displayName,
                material,
                stats,
                section.getBoolean("dyeable", false),
                data,
                model,
                bones,
                items
        ));
    }

    /**
     * Find the armor bones among a model's groups, and warn about pivots that are off.
     *
     * @param model the model
     * @param report where problems are noted
     * @return the first group of each armor bone
     */
    private static Map<ArmorBone, Group> bones(final ModelData model, final LoadReport report) {
        Map<ArmorBone, Group> bones = new EnumMap<>(ArmorBone.class);
        for (Group group : model.groups().values()) {
            Optional<ArmorBone> bone = ArmorBone.fromName(group.name());
            if (bone.isEmpty()) {
                continue;
            }

            if (bones.containsKey(bone.get())) {
                report.warn("bone " + group.name(), String.format(TWICE, bone.get().bone()));
                continue;
            }

            bones.put(bone.get(), group);
            Vector3fc pivot = bone.get().pivot();
            ArmorPart part = bone.get().part();
            String limb = part == null ? "head" : part.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            if (group.origin().distance(pivot) > PIVOT_TOLERANCE) {
                report.warn("bone " + group.name(), String.format(PIVOT, describe(group.origin()), limb, describe(pivot)));
            }
        }

        return bones;
    }

    /**
     * Warn about flat textures that are not twice as wide as high.
     *
     * @param model the model
     * @param report where problems are noted
     */
    private static void checkFlatTextures(final ModelData model, final LoadReport report) {
        ArmorTextures roles = new ArmorTextures(model.textures());
        for (ArmorLayer layer : ArmorLayer.values()) {
            for (OptionalInt index : List.of(roles.flat(layer), roles.flatTint(layer), roles.flatGlow(layer))) {
                if (index.isEmpty()) {
                    continue;
                }

                Texture texture = model.textures().get(index.getAsInt());
                if (texture.width() != texture.height() / Math.max(1, texture.frames()) * LAYER_ASPECT) {
                    report.warn("texture " + texture.name(), String.format(FLAT_SIZE, texture.width(), texture.height()));
                }
            }
        }
    }

    /**
     * Read the vanilla armor a set is made from.
     *
     * @param section the set's settings
     * @param report where problems are noted
     * @return the material, netherite when unset or unknown
     */
    private static ArmorMaterial material(final ConfigurationSection section, final LoadReport report) {
        String base = section.getString("base", "netherite");
        Optional<ArmorMaterial> material = ArmorMaterial.fromName(base);
        if (material.isEmpty()) {
            report.warn("base", String.format(BAD_BASE, base));
        }

        return material.orElse(ArmorMaterial.NETHERITE);
    }

    /**
     * Read the stats of each piece: the given armor points, or the base's, and the toughness and
     * knockback resistance shared by every piece.
     *
     * @param section the set's settings
     * @param material the base
     * @param report where problems are noted
     * @return the stats by slot
     */
    private static Map<ArmorSlot, ArmorStats> stats(
            final ConfigurationSection section,
            final ArmorMaterial material,
            final LoadReport report
    ) {
        List<Double> armor = section.isList("armor") ? section.getDoubleList("armor") : List.of();
        if (section.contains("armor") && armor.size() != SLOTS) {
            report.warn("armor", BAD_ARMOR);
            armor = List.of();
        }

        Map<ArmorSlot, ArmorStats> stats = new EnumMap<>(ArmorSlot.class);
        ArmorSlot[] slots = ArmorSlot.values();
        for (int index = 0; index < slots.length; index++) {
            ArmorSlot slot = slots[index];
            ArmorStats base = material.stats(slot);
            stats.put(slot, new ArmorStats(
                    armor.isEmpty() ? base.armor() : armor.get(index),
                    section.getDouble("toughness", base.toughness()),
                    section.getDouble("knockback-resistance", base.knockbackResistance())
            ));
        }

        return stats;
    }

    /**
     * Read a section's values, nested sections as maps.
     *
     * @param section the section, or null
     * @return the values, empty for null
     */
    private static Map<String, Object> values(final @Nullable ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (section == null) {
            return values;
        }

        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nested) {
                values.put(key, values(nested));
            } else if (value != null) {
                values.put(key, value);
            }
        }

        return values;
    }

    /**
     * Make a name for players from a key's value: {@code sets/ice_king} becomes
     * {@code Ice King}.
     *
     * @param value the key's value
     * @return the name
     */
    static String title(final String value) {
        String last = value.substring(value.lastIndexOf('/') + 1);
        StringBuilder title = new StringBuilder();
        for (String word : WORDS.splitAsStream(last).toList()) {
            if (word.isEmpty()) {
                continue;
            }

            if (!title.isEmpty()) {
                title.append(' ');
            }

            title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return title.toString();
    }

    /**
     * Describe a point for a message.
     *
     * @param point the point
     * @return the point as {@code [x, y, z]}
     */
    private static String describe(final Vector3fc point) {
        return String.format(Locale.ROOT, "[%s, %s, %s]", trim(point.x()), trim(point.y()), trim(point.z()));
    }

    /**
     * Write a number without a trailing {@code .0}.
     *
     * @param value the number
     * @return the text
     */
    private static String trim(final float value) {
        return value == (int) value ? String.valueOf((int) value) : String.valueOf(value);
    }

    /**
     * Log every issue of a report.
     *
     * @param report the report
     * @param logger the logger
     */
    private static void log(final LoadReport report, final Logger logger) {
        for (LoadIssue issue : report.issues()) {
            Level level = issue.severity() == IssueSeverity.ERROR ? Level.SEVERE : Level.WARNING;
            logger.log(level, String.format(LOG, report.source(), issue.describe()));
        }
    }
}
