package dev.vertcode.hyperion.api.model.behavior;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;

/**
 * Decides whether a bone gets a behavior.
 *
 * <p>Two sources work side by side. Name prefixes work in plain Blockbench today
 * ({@code h_head}, {@code hitbox}). Tags are written into the bone's metadata under
 * {@link #TAG_FIELD}, which the Hyperion Blockbench plugin will set from a menu.
 */
@FunctionalInterface
public interface BehaviorMatcher {

    /**
     * The metadata field on a group that lists its behavior tags.
     */
    String TAG_FIELD = "hyperion_behaviors";

    /**
     * Match bones whose name starts with any of the prefixes, ignoring case.
     *
     * @param prefixes the name prefixes
     * @return the matcher
     */
    static BehaviorMatcher namePrefix(final String... prefixes) {
        List<String> lowered = Arrays.stream(prefixes).map(prefix -> prefix.toLowerCase(Locale.ROOT)).toList();

        return bone -> {
            String name = bone.name().toLowerCase(Locale.ROOT);
            for (String prefix : lowered) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }

            return false;
        };
    }

    /**
     * Match bones whose name is exactly the given name, ignoring case.
     *
     * @param name the bone name
     * @return the matcher
     */
    static BehaviorMatcher nameEquals(final String name) {
        return bone -> bone.name().equalsIgnoreCase(name);
    }

    /**
     * Match bones whose {@link #TAG_FIELD} metadata contains the tag.
     *
     * @param tag the tag, such as {@code hyperion:seat}
     * @return the matcher
     */
    static BehaviorMatcher tagged(final String tag) {
        return bone -> bone.metadata().strings(TAG_FIELD).contains(tag);
    }

    /**
     * Decide whether a bone gets the behavior.
     *
     * @param bone the bone's blueprint
     * @return true when the bone gets the behavior
     */
    boolean matches(BoneBlueprint bone);

    /**
     * Combine with another matcher: a bone matches when either matches.
     *
     * @param other the other matcher
     * @return the combined matcher
     */
    default BehaviorMatcher or(final BehaviorMatcher other) {
        return bone -> this.matches(bone) || other.matches(bone);
    }

    /**
     * Combine with another matcher: a bone matches when both match.
     *
     * @param other the other matcher
     * @return the combined matcher
     */
    default BehaviorMatcher and(final BehaviorMatcher other) {
        return bone -> this.matches(bone) && other.matches(bone);
    }
}
