package dev.vertcode.hyperion.debug;

import java.util.Map;

import org.bukkit.Location;

/**
 * What the effects view draws of one running effect this tick.
 *
 * @param key the effect's key
 * @param tick the tick it is at
 * @param duration how many ticks it lasts
 * @param anchor where it is played
 * @param ats where each {@code at} its parts use resolves to, such as {@code locator:mouth}
 * @param parts where each part that has a spot is, by name
 */
public record DebugEffect(
        String key,
        int tick,
        int duration,
        Location anchor,
        Map<String, Location> ats,
        Map<String, Location> parts
) {
}
