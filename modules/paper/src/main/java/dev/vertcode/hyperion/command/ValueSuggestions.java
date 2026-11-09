package dev.vertcode.hyperion.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.entity.EntityType;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.util.sender.Source;

/**
 * Suggest values for the optional arguments and flags, so the client shows them while typing:
 * common scales, speeds and radii, and the mobs a model can be spawned on.
 */
public final class ValueSuggestions {

    /**
     * The name of the scale suggestions.
     */
    public static final String SCALES = "scales";

    /**
     * The name of the animation speed suggestions.
     */
    public static final String SPEEDS = "speeds";

    /**
     * The name of the radius suggestions.
     */
    public static final String RADII = "radii";

    /**
     * The name of the mob suggestions.
     */
    public static final String MOBS = "mobs";

    private static final List<String> SCALE_VALUES = List.of("0.5", "0.75", "1", "1.5", "2", "3");
    private static final List<String> SPEED_VALUES = List.of("0.5", "1", "1.5", "2");
    private static final List<String> RADIUS_VALUES = List.of("8", "16", "32", "64");

    private final List<String> mobs = mobs();

    /**
     * Suggest common scales.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the scales
     */
    @Suggestions(SCALES)
    public List<String> scales(final CommandContext<Source> context, final String input) {
        return SCALE_VALUES;
    }

    /**
     * Suggest common animation speeds.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the speeds
     */
    @Suggestions(SPEEDS)
    public List<String> speeds(final CommandContext<Source> context, final String input) {
        return SPEED_VALUES;
    }

    /**
     * Suggest common radii.
     *
     * @param context the command being typed
     * @param input what has been typed for this argument
     * @return the radii in blocks
     */
    @Suggestions(RADII)
    public List<String> radii(final CommandContext<Source> context, final String input) {
        return RADIUS_VALUES;
    }

    /**
     * Suggest every mob a model can be spawned on.
     *
     * @param context the command being typed
     * @param input what has been typed for this flag
     * @return the mobs' names
     */
    @Suggestions(MOBS)
    public List<String> mobs(final CommandContext<Source> context, final String input) {
        return this.mobs;
    }

    /**
     * List the living entity types that can be spawned, players left out.
     *
     * @return their names, sorted
     */
    private static List<String> mobs() {
        List<String> names = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && type.isSpawnable() && type != EntityType.PLAYER && type != EntityType.UNKNOWN) {
                names.add(type.getKey().getKey().toLowerCase(Locale.ROOT));
            }
        }

        names.sort(String::compareTo);

        return List.copyOf(names);
    }
}
