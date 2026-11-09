package dev.vertcode.hyperion.effect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.Easing;
import dev.vertcode.hyperion.api.effect.Effect;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.api.effect.EffectKeyframe;
import dev.vertcode.hyperion.api.effect.EffectPart;
import dev.vertcode.hyperion.api.effect.EffectViewers;
import dev.vertcode.hyperion.blueprint.LoadReport;
import dev.vertcode.hyperion.effect.part.ParticleData;

/**
 * Read effects from a tree: a YAML file's sections, or the {@code hyperion_effects} of a model.
 *
 * <pre>{@code
 * fire_breath:
 *   duration: 40
 *   viewers: nearby:32
 *   parameters: { color: "#FF5500" }
 *   parts:
 *     flames:
 *       type: particle
 *       at: locator:mouth
 *       particle: FLAME
 *       keyframes:
 *         0: { visible: false }
 *         8: { visible: true, radius: 0.6, easing: ease-out }
 * }</pre>
 *
 * <p>A value it cannot use is a warning in the report and is left out, so one typo never loses a
 * whole effect; an effect without a usable duration or parts is an error.
 */
public final class EffectLoader {

    private static final int DEFAULT_RADIUS = 32;
    private static final String NOT_A_SECTION = "is not a section of keys";
    private static final String BAD_DURATION = "needs a duration of at least 1 tick";
    private static final String BAD_VIEWERS = "has viewers '%s'; use model, everyone or nearby:<radius>";
    private static final String NO_PARTS = "has no parts";
    private static final String BAD_TYPE = "has type '%s', which is not a part type";
    private static final String UNKNOWN_TYPE = "has type %s, which no plugin registered; it is skipped";
    private static final String BAD_TICK = "has a keyframe at '%s', which is not a tick";
    private static final String BAD_EASING = "has easing '%s', which Hyperion does not know; it eases linearly";
    private static final String BAD_VALUE = "has '%s: %s', which is not text, a number, a switch or [x, y, z]";
    private static final String BAD_PARTICLE = "uses particle '%s', which this server does not have";
    private static final String UNSUPPORTED_PARTICLE = "uses particle '%s', which needs data effects cannot give";

    private EffectLoader() {
    }

    /**
     * Read one effect.
     *
     * @param key the effect's key
     * @param tree the effect's section
     * @param report where problems go
     * @param knownType tells whether a part type is registered
     * @return the effect, or empty when it has an error
     */
    public static Optional<Effect> effect(
            final Key key,
            final Object tree,
            final LoadReport report,
            final Predicate<Key> knownType
    ) {
        String where = "effect " + key.asString();
        if (!(tree instanceof Map<?, ?> section)) {
            report.error(where, NOT_A_SECTION);
            return Optional.empty();
        }

        int duration = section.get("duration") instanceof Number number ? number.intValue() : 0;
        if (duration < 1) {
            report.error(where, BAD_DURATION);
            return Optional.empty();
        }

        EffectViewers viewers = viewers(section.get("viewers"), where, report);
        Map<String, String> parameters = new LinkedHashMap<>();
        if (section.get("parameters") instanceof Map<?, ?> values) {
            values.forEach((name, value) -> parameters.put(String.valueOf(name), String.valueOf(value)));
        }

        List<EffectPart> parts = new ArrayList<>();
        if (section.get("parts") instanceof Map<?, ?> partTrees) {
            for (Map.Entry<?, ?> entry : partTrees.entrySet()) {
                String name = String.valueOf(entry.getKey());
                part(name, entry.getValue(), where + ", part " + name, report, knownType).ifPresent(parts::add);
            }
        }

        if (parts.isEmpty()) {
            report.error(where, NO_PARTS);
            return Optional.empty();
        }

        boolean loop = section.get("loop") instanceof Boolean flag && flag;

        return Optional.of(new Effect(key, duration, loop, viewers, parameters, parts));
    }

    /**
     * Read who sees an effect.
     *
     * @param value the {@code viewers} value, or null for the default
     * @param where the effect, for warnings
     * @param report where problems go
     * @return the viewers
     */
    private static EffectViewers viewers(final @Nullable Object value, final String where, final LoadReport report) {
        if (value == null) {
            return EffectViewers.model();
        }

        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (text.equals("model")) {
            return EffectViewers.model();
        }

        if (text.equals("everyone") || text.equals("all")) {
            return EffectViewers.everyone();
        }

        if (text.equals("nearby")) {
            return EffectViewers.nearby(DEFAULT_RADIUS);
        }

        if (text.startsWith("nearby:")) {
            try {
                return EffectViewers.nearby(Double.parseDouble(text.substring("nearby:".length())));
            } catch (IllegalArgumentException exception) {
                // Reported below, with the other values it cannot read.
            }
        }

        report.warn(where, String.format(BAD_VIEWERS, value));

        return EffectViewers.model();
    }

    /**
     * Read one part.
     *
     * @param name the part's name
     * @param tree the part's section
     * @param where the part, for warnings
     * @param report where problems go
     * @param knownType tells whether a part type is registered
     * @return the part, or empty when it cannot be used
     */
    private static Optional<EffectPart> part(
            final String name,
            final Object tree,
            final String where,
            final LoadReport report,
            final Predicate<Key> knownType
    ) {
        if (!(tree instanceof Map<?, ?> section)) {
            report.warn(where, NOT_A_SECTION);
            return Optional.empty();
        }

        String typeName = String.valueOf(section.get("type") == null ? "particle" : section.get("type")).trim();
        Key type;
        try {
            type = typeName.contains(":") ? Key.key(typeName) : Key.key("hyperion", typeName.toLowerCase(Locale.ROOT));
        } catch (InvalidKeyException exception) {
            report.warn(where, String.format(BAD_TYPE, typeName));
            return Optional.empty();
        }

        if (!knownType.test(type)) {
            report.warn(where, String.format(UNKNOWN_TYPE, type.asString()));
            return Optional.empty();
        }

        Map<String, Object> settings = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : section.entrySet()) {
            String field = String.valueOf(entry.getKey());
            if (!field.equals("type") && !field.equals("keyframes")) {
                put(settings, field, entry.getValue(), where, report);
            }
        }

        List<EffectKeyframe> keyframes = new ArrayList<>();
        if (section.get("keyframes") instanceof Map<?, ?> frames) {
            for (Map.Entry<?, ?> entry : frames.entrySet()) {
                keyframe(String.valueOf(entry.getKey()), entry.getValue(), where, report).ifPresent(keyframes::add);
            }
        }

        EffectPart part = new EffectPart(name, type, new EffectFrame(settings), keyframes);
        checkParticles(part, where, report);

        return Optional.of(part);
    }

    /**
     * Read one keyframe.
     *
     * @param tickText the keyframe's tick, as written
     * @param tree the keyframe's values
     * @param where the part, for warnings
     * @param report where problems go
     * @return the keyframe, or empty when its tick cannot be read
     */
    private static Optional<EffectKeyframe> keyframe(
            final String tickText,
            final Object tree,
            final String where,
            final LoadReport report
    ) {
        int tick;
        try {
            tick = Integer.parseInt(tickText.trim());
        } catch (NumberFormatException exception) {
            tick = -1;
        }

        if (tick < 0 || !(tree instanceof Map<?, ?> section)) {
            report.warn(where, String.format(BAD_TICK, tickText));
            return Optional.empty();
        }

        Easing easing = Easing.LINEAR;
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : section.entrySet()) {
            String field = String.valueOf(entry.getKey());
            if (field.equals("easing")) {
                String name = String.valueOf(entry.getValue());
                easing = Easing.fromKey(name).orElseGet(() -> {
                    report.warn(where, String.format(BAD_EASING, name));
                    return Easing.LINEAR;
                });
            } else {
                put(values, field, entry.getValue(), where, report);
            }
        }

        return Optional.of(new EffectKeyframe(tick, new EffectFrame(values), easing));
    }

    /**
     * Turn one value into what a frame holds: numbers become floats and three numbers a vector.
     *
     * @param into where it goes
     * @param field the value's name
     * @param value the value
     * @param where the part, for warnings
     * @param report where problems go
     */
    private static void put(
            final Map<String, Object> into,
            final String field,
            final @Nullable Object value,
            final String where,
            final LoadReport report
    ) {
        Object kept = switch (value) {
            case Number number -> number.floatValue();
            case Boolean flag -> flag;
            case String text -> text;
            case List<?> list when list.size() == 3 && list.stream().allMatch(Number.class::isInstance) -> new Vector3f(
                    ((Number) list.get(0)).floatValue(),
                    ((Number) list.get(1)).floatValue(),
                    ((Number) list.get(2)).floatValue()
            );
            case null, default -> null;
        };

        if (kept == null) {
            report.warn(where, String.format(BAD_VALUE, field, value));
            return;
        }

        into.put(field, kept);
    }

    /**
     * Warn about particle names the server does not have, or that need data effects cannot
     * give. Names with a {@code %parameter%} are checked when they play.
     *
     * @param part the part
     * @param where the part, for warnings
     * @param report where problems go
     */
    private static void checkParticles(final EffectPart part, final String where, final LoadReport report) {
        List<EffectFrame> frames = new ArrayList<>();
        frames.add(part.settings());
        part.keyframes().forEach(keyframe -> frames.add(keyframe.frame()));
        for (EffectFrame frame : frames) {
            String name = frame.string("particle").orElse(null);
            if (name == null || name.contains("%")) {
                continue;
            }

            ParticleData.Lookup found = ParticleData.lookup(name);
            if (found == ParticleData.Lookup.UNKNOWN) {
                report.warn(where, String.format(BAD_PARTICLE, name));
            } else if (found == ParticleData.Lookup.UNSUPPORTED) {
                report.warn(where, String.format(UNSUPPORTED_PARTICLE, name));
            }
        }
    }
}
