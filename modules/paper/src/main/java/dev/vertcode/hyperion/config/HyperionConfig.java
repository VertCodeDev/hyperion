package dev.vertcode.hyperion.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Logger;

import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.behavior.builtin.BuiltinBehaviors;
import dev.vertcode.hyperion.api.model.hitbox.HitboxMode;

/**
 * Hyperion's settings from {@code config.yml}.
 *
 * <p>Immutable: a reload reads a new one. A value that cannot be used falls back to its default
 * with a warning that names the key, so one typo never stops the plugin.
 *
 * @param pack the resource pack settings
 * @param models the settings live models start with
 * @param bedrock the Bedrock settings
 * @param effects the effect settings
 * @param debug how the debug views draw
 * @param gallery the gallery settings
 * @param armor how custom armor is drawn
 */
public record HyperionConfig(
        PackSettings pack,
        ModelSettings models,
        BedrockSettings bedrock,
        EffectSettings effects,
        DebugSettings debug,
        GallerySettings gallery,
        ArmorSettings armor
) {

    /**
     * The file name, in the data folder and in the jar.
     */
    public static final String FILE = "config.yml";

    private static final String BAD_VALUE = "config.yml: '%s' is not a valid %s, using %s";
    private static final int DEFAULT_PORT = 8163;
    private static final int DEFAULT_LINK_PORT = 8164;
    private static final int MAX_PORT = 65_535;
    private static final int DEFAULT_CUSTOM_MODEL_DATA = 10_000;
    private static final String DEFAULT_BASE_ITEM = "paper";
    private static final String MINECRAFT = "minecraft:";
    private static final int DEFAULT_VIEW_DISTANCE = 48;
    private static final int DEFAULT_INTERPOLATION = 2;
    private static final String AUTO = "models.auto-animations.";
    private static final String BEHAVIORS = "behaviors.";
    private static final String STEERING = "behaviors.seats.steering.";
    private static final float DEFAULT_MAX_YAW = 50f;
    private static final float DEFAULT_MAX_PITCH = 40f;
    private static final float DEFAULT_SPEED = 0.25f;
    private static final float DEFAULT_JUMP = 0.5f;
    private static final float MAX_YAW = 180f;
    private static final float MAX_PITCH = 90f;
    private static final Map<String, Key> BEHAVIOR_NAMES = behaviorNames();
    private static final int DEFAULT_EFFECT_DISTANCE = 32;
    private static final int DEFAULT_DEBUG_INTERVAL = 4;
    private static final float DEFAULT_AXIS_LENGTH = 0.4f;
    private static final int DEFAULT_PREVIEW_SECONDS = 120;
    private static final int DEFAULT_ARMOR_DISTANCE = 32;
    private static final int DEFAULT_MAX_LAYERS = 16;
    private static final int MOST_LAYERS = 64;

    /**
     * Read {@code config.yml} from the data folder, writing or completing it from the jar first.
     *
     * @param plugin the plugin whose data folder and jar to read
     * @return the settings
     */
    public static HyperionConfig load(final Plugin plugin) {
        return of(BundledYaml.load(plugin, FILE), plugin.getLogger(), HyperionConfig::isItem);
    }

    /**
     * Read the settings from a loaded file.
     *
     * @param config the contents of {@code config.yml}
     * @param logger where unusable values are reported
     * @param isItem tells whether a name, such as {@code paper}, is an item on this server
     * @return the settings
     */
    public static HyperionConfig of(
            final ConfigurationSection config,
            final Logger logger,
            final Predicate<String> isItem
    ) {
        String hostingKey = config.getString("pack.hosting", PackHosting.SELF_HOST.key());
        PackHosting hosting = PackHosting.fromKey(hostingKey).orElseGet(() -> {
            logger.warning(String.format(BAD_VALUE, hostingKey, "pack.hosting", PackHosting.SELF_HOST.key()));
            return PackHosting.SELF_HOST;
        });

        int port = config.getInt("pack.self-host.port", DEFAULT_PORT);
        if (port < 1 || port > MAX_PORT) {
            logger.warning(String.format(BAD_VALUE, port, "pack.self-host.port", DEFAULT_PORT));
            port = DEFAULT_PORT;
        }

        int viewDistance = config.getInt("models.view-distance", DEFAULT_VIEW_DISTANCE);
        if (viewDistance < 1) {
            logger.warning(String.format(BAD_VALUE, viewDistance, "models.view-distance", DEFAULT_VIEW_DISTANCE));
            viewDistance = DEFAULT_VIEW_DISTANCE;
        }

        int interpolation = config.getInt("models.interpolation", DEFAULT_INTERPOLATION);
        if (interpolation < 0) {
            logger.warning(String.format(BAD_VALUE, interpolation, "models.interpolation", DEFAULT_INTERPOLATION));
            interpolation = DEFAULT_INTERPOLATION;
        }

        PackSettings pack = new PackSettings(
                hosting,
                port,
                config.getString("pack.self-host.address", "").trim(),
                config.getString("pack.url", "").trim(),
                config.getBoolean("pack.send-on-join", true),
                config.getBoolean("pack.required", false),
                config.getBoolean("pack.resend-on-change", true),
                baseItem(config.getString("pack.base-item", DEFAULT_BASE_ITEM), logger, isItem),
                config.getInt("pack.custom-model-data-start", DEFAULT_CUSTOM_MODEL_DATA)
        );

        String spawnEntity = config.getString("models.spawn-entity", "pig").trim().toLowerCase(Locale.ROOT);
        if (spawnEntity.startsWith(MINECRAFT)) {
            spawnEntity = spawnEntity.substring(MINECRAFT.length());
        }

        ModelSettings models = new ModelSettings(
                viewDistance,
                interpolation,
                autoAnimations(config),
                spawnEntity,
                behaviors(config, logger)
        );

        int linkPort = config.getInt("bedrock.link-port", DEFAULT_LINK_PORT);
        if (linkPort < 1 || linkPort > MAX_PORT) {
            logger.warning(String.format(BAD_VALUE, linkPort, "bedrock.link-port", DEFAULT_LINK_PORT));
            linkPort = DEFAULT_LINK_PORT;
        }

        BedrockSettings bedrock = new BedrockSettings(config.getBoolean("bedrock.enabled", true), linkPort);

        int effectDistance = config.getInt("effects.view-distance", DEFAULT_EFFECT_DISTANCE);
        if (effectDistance < 1) {
            logger.warning(String.format(BAD_VALUE, effectDistance, "effects.view-distance", DEFAULT_EFFECT_DISTANCE));
            effectDistance = DEFAULT_EFFECT_DISTANCE;
        }

        float multiplier = (float) Math.max(0d, config.getDouble("effects.particle-multiplier", 1d));
        EffectSettings effects = new EffectSettings(effectDistance, multiplier);

        return new HyperionConfig(
                pack,
                models,
                bedrock,
                effects,
                debug(config, logger),
                gallery(config, logger),
                armor(config, logger)
        );
    }

    /**
     * Read how the debug views draw.
     *
     * @param config the contents of {@code config.yml}
     * @param logger where unusable values are reported
     * @return the settings
     */
    private static DebugSettings debug(final ConfigurationSection config, final Logger logger) {
        int interval = config.getInt("debug.interval", DEFAULT_DEBUG_INTERVAL);
        if (interval < 1) {
            logger.warning(String.format(BAD_VALUE, interval, "debug.interval", DEFAULT_DEBUG_INTERVAL));
            interval = DEFAULT_DEBUG_INTERVAL;
        }

        float axisLength = (float) config.getDouble("debug.axis-length", DEFAULT_AXIS_LENGTH);
        if (!(axisLength > 0f)) {
            logger.warning(String.format(BAD_VALUE, axisLength, "debug.axis-length", DEFAULT_AXIS_LENGTH));
            axisLength = DEFAULT_AXIS_LENGTH;
        }

        return new DebugSettings(interval, config.getBoolean("debug.labels", true), axisLength);
    }

    /**
     * Read how custom armor is drawn.
     *
     * @param config the contents of {@code config.yml}
     * @param logger where unusable values are reported
     * @return the settings
     */
    private static ArmorSettings armor(final ConfigurationSection config, final Logger logger) {
        int distance = config.getInt("armor.render-distance", DEFAULT_ARMOR_DISTANCE);
        if (distance < 1) {
            logger.warning(String.format(BAD_VALUE, distance, "armor.render-distance", DEFAULT_ARMOR_DISTANCE));
            distance = DEFAULT_ARMOR_DISTANCE;
        }

        int layers = config.getInt("armor.max-layers", DEFAULT_MAX_LAYERS);
        if (layers < 1 || layers > MOST_LAYERS) {
            logger.warning(String.format(BAD_VALUE, layers, "armor.max-layers", DEFAULT_MAX_LAYERS));
            layers = DEFAULT_MAX_LAYERS;
        }

        return new ArmorSettings(config.getBoolean("armor.shader", false), distance, layers);
    }

    /**
     * Read the gallery settings.
     *
     * @param config the contents of {@code config.yml}
     * @param logger where unusable values are reported
     * @return the settings
     */
    private static GallerySettings gallery(final ConfigurationSection config, final Logger logger) {
        int seconds = config.getInt("gallery.preview-seconds", DEFAULT_PREVIEW_SECONDS);
        if (seconds < 1) {
            logger.warning(String.format(BAD_VALUE, seconds, "gallery.preview-seconds", DEFAULT_PREVIEW_SECONDS));
            seconds = DEFAULT_PREVIEW_SECONDS;
        }

        return new GallerySettings(seconds);
    }

    /**
     * Read the automatic idle and walk animations: the default, the names, and a choice per
     * entity type.
     *
     * @param config the contents of {@code config.yml}
     * @return the settings
     */
    private static AutoAnimations autoAnimations(final ConfigurationSection config) {
        return new AutoAnimations(
                config.getBoolean(AUTO + "enabled", true),
                config.getString(AUTO + "idle", "idle"),
                config.getString(AUTO + "walk", "walk"),
                entityChoices(config.getConfigurationSection(AUTO + "entities"))
        );
    }

    /**
     * Read a choice per entity type, such as {@code armor_stand: false}.
     *
     * @param section the section, or null when the file has none
     * @return the choices by type name, without the namespace
     */
    private static Map<String, Boolean> entityChoices(final @Nullable ConfigurationSection section) {
        Map<String, Boolean> entities = new HashMap<>();
        if (section == null) {
            return entities;
        }

        for (String key : section.getKeys(false)) {
            String type = key.trim().toLowerCase(Locale.ROOT);
            if (type.startsWith(MINECRAFT)) {
                type = type.substring(MINECRAFT.length());
            }

            entities.put(type, section.getBoolean(key));
        }

        return entities;
    }

    /**
     * Read the built-in behaviors: the bone name prefixes of each, the head's limits, the hitbox
     * mode and the seats.
     *
     * @param config the contents of {@code config.yml}
     * @param logger where unusable values are reported
     * @return the settings
     */
    private static BehaviorSettings behaviors(final ConfigurationSection config, final Logger logger) {
        Map<Key, List<String>> prefixes = new HashMap<>();
        for (Map.Entry<String, Key> behavior : BEHAVIOR_NAMES.entrySet()) {
            List<String> names = new ArrayList<>();
            for (String prefix : config.getStringList(BEHAVIORS + "prefixes." + behavior.getKey())) {
                String lowered = prefix.trim().toLowerCase(Locale.ROOT);
                if (!lowered.isEmpty()) {
                    names.add(lowered);
                }
            }

            prefixes.put(behavior.getValue(), List.copyOf(names));
        }

        String modeKey = config.getString(BEHAVIORS + "hitboxes.mode", "bones").trim().toUpperCase(Locale.ROOT);
        HitboxMode mode;
        try {
            mode = HitboxMode.valueOf(modeKey);
        } catch (IllegalArgumentException exception) {
            logger.warning(String.format(BAD_VALUE, modeKey, "behaviors.hitboxes.mode", "bones"));
            mode = HitboxMode.BONES;
        }

        String steeringMode = config.getString(STEERING + "mode", "walking").trim().toLowerCase(Locale.ROOT);
        if (!steeringMode.equals("walking") && !steeringMode.equals("flying")) {
            logger.warning(String.format(BAD_VALUE, steeringMode, "behaviors.seats.steering.mode", "walking"));
            steeringMode = "walking";
        }

        float speed = (float) config.getDouble(STEERING + "speed", DEFAULT_SPEED);
        if (!(speed > 0f)) {
            logger.warning(String.format(BAD_VALUE, speed, "behaviors.seats.steering.speed", DEFAULT_SPEED));
            speed = DEFAULT_SPEED;
        }

        SeatSteering steering = new SeatSteering(
                config.getBoolean(STEERING + "enabled", false),
                steeringMode.equals("flying"),
                speed,
                (float) Math.max(0d, config.getDouble(STEERING + "jump", DEFAULT_JUMP)),
                entityChoices(config.getConfigurationSection(STEERING + "entities"))
        );

        return new BehaviorSettings(
                prefixes,
                (float) Math.clamp(config.getDouble(BEHAVIORS + "head.max-yaw", DEFAULT_MAX_YAW), 0d, MAX_YAW),
                (float) Math.clamp(config.getDouble(BEHAVIORS + "head.max-pitch", DEFAULT_MAX_PITCH), 0d, MAX_PITCH),
                mode,
                config.getBoolean(BEHAVIORS + "hitboxes.forward-damage", true),
                config.getBoolean(BEHAVIORS + "seats.click-to-ride", true),
                steering
        );
    }

    /**
     * Map each built-in behavior's name in {@code config.yml} to its key.
     *
     * @return the keys by name, such as {@code main-hand}
     */
    private static Map<String, Key> behaviorNames() {
        Map<String, Key> names = new LinkedHashMap<>();
        names.put("head", BuiltinBehaviors.HEAD);
        names.put("hitbox", BuiltinBehaviors.HITBOX);
        names.put("seat", BuiltinBehaviors.SEAT);
        names.put("nametag", BuiltinBehaviors.NAMETAG);
        names.put("main-hand", BuiltinBehaviors.MAIN_HAND);
        names.put("off-hand", BuiltinBehaviors.OFF_HAND);
        names.put("ghost", BuiltinBehaviors.GHOST);

        return Map.copyOf(names);
    }

    /**
     * Read the base item, which must be an item rather than a block-only material.
     *
     * @param name the item's name, such as {@code paper} or {@code minecraft:paper}
     * @param logger where an unusable name is reported
     * @param isItem tells whether a name is an item on this server
     * @return the item's name without the namespace, {@code paper} when the name is not an item
     */
    private static String baseItem(final String name, final Logger logger, final Predicate<String> isItem) {
        String item = name.trim().toLowerCase(Locale.ROOT);
        if (item.startsWith(MINECRAFT)) {
            item = item.substring(MINECRAFT.length());
        }

        if (!isItem.test(item)) {
            logger.warning(String.format(BAD_VALUE, name, "pack.base-item", DEFAULT_BASE_ITEM));
            return DEFAULT_BASE_ITEM;
        }

        return item;
    }

    /**
     * Return whether a name is an item on the running server.
     *
     * @param name the item's name, such as {@code paper}
     * @return true for an item, false for air, a block-only material or an unknown name
     */
    private static boolean isItem(final String name) {
        Material material = Material.matchMaterial(name);

        return material != null && material.isItem() && !material.isAir();
    }
}
