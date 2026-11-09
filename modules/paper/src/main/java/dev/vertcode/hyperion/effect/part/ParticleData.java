package dev.vertcode.hyperion.effect.part;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.EffectFrame;

/**
 * Find particles by name, build the data some of them need, and send them to players.
 *
 * <p>Particles are found by their key in the server's registry, such as {@code flame} or
 * {@code minecraft:flame}, any case. Dust takes {@code color} and {@code size}; dust that changes
 * color also takes {@code to-color}; particles with a color take {@code color}. Particles that need
 * an item or a block are not supported.
 */
public final class ParticleData {

    private static final String MINECRAFT = "minecraft:";
    private static final float DEFAULT_SIZE = 1f;
    private static final Map<String, Optional<Particle>> FOUND = new ConcurrentHashMap<>();

    private ParticleData() {
    }

    /**
     * What looking a name up found.
     */
    public enum Lookup {

        /**
         * A particle effects can send.
         */
        FOUND,

        /**
         * A particle that needs data effects cannot give.
         */
        UNSUPPORTED,

        /**
         * No particle by that name.
         */
        UNKNOWN
    }

    /**
     * Find a particle by name.
     *
     * @param name the name, such as {@code FLAME} or {@code minecraft:flame}
     * @return the particle, or null when the server has none by that name
     */
    public static @Nullable Particle particle(final String name) {
        return FOUND.computeIfAbsent(name, ParticleData::find).orElse(null);
    }

    /**
     * Look a particle up in the server's registry.
     *
     * @param name the name
     * @return the particle, or empty
     */
    private static Optional<Particle> find(final String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith(MINECRAFT)) {
            key = key.substring(MINECRAFT.length());
        }

        for (Particle particle : Registry.PARTICLE_TYPE) {
            if (particle.getKey().getKey().equals(key)) {
                return Optional.of(particle);
            }
        }

        return Optional.empty();
    }

    /**
     * Check a name: whether it is a particle, and whether effects can send it.
     *
     * @param name the name
     * @return what the lookup found
     */
    public static Lookup lookup(final String name) {
        Particle particle = particle(name);
        if (particle == null) {
            return Lookup.UNKNOWN;
        }

        return supported(particle) ? Lookup.FOUND : Lookup.UNSUPPORTED;
    }

    /**
     * Return whether effects can send a particle: it needs no data, or dust or a color.
     *
     * @param particle the particle
     * @return true when supported
     */
    public static boolean supported(final Particle particle) {
        Class<?> type = particle.getDataType();

        return type == Void.class
                || type == Particle.DustOptions.class
                || type == Particle.DustTransition.class
                || type == Color.class;
    }

    /**
     * Build the data a particle needs from a frame's {@code color}, {@code to-color} and
     * {@code size}.
     *
     * @param particle the particle
     * @param frame the values
     * @return the data, or null for a particle that needs none
     */
    public static @Nullable Object data(final Particle particle, final EffectFrame frame) {
        Class<?> type = particle.getDataType();
        Color color = color(frame.string("color", "#FFFFFF"));
        float size = frame.number("size", DEFAULT_SIZE);
        if (type == Particle.DustOptions.class) {
            return new Particle.DustOptions(color, size);
        }

        if (type == Particle.DustTransition.class) {
            return new Particle.DustTransition(color, color(frame.string("to-color", frame.string("color", "#FFFFFF"))), size);
        }

        return type == Color.class ? color : null;
    }

    /**
     * Read a color as {@code #RRGGBB}, {@code RRGGBB} or {@code r,g,b}.
     *
     * @param text the color
     * @return the color, white when it cannot be read
     */
    static Color color(final String text) {
        String value = text.trim();
        try {
            if (value.contains(",")) {
                String[] parts = value.split(",", -1);
                return Color.fromRGB(
                        Math.clamp(Integer.parseInt(parts[0].trim()), 0, 255),
                        Math.clamp(Integer.parseInt(parts[1].trim()), 0, 255),
                        Math.clamp(Integer.parseInt(parts[2].trim()), 0, 255)
                );
            }

            String hex = value.startsWith("#") ? value.substring(1) : value;
            return Color.fromRGB(Integer.parseInt(hex, 16) & 0xFFFFFF);
        } catch (RuntimeException exception) {
            return Color.WHITE;
        }
    }

    /**
     * Send particles to players: spread around a spot, or, with a direction, each one flying
     * that way at the speed.
     *
     * @param viewers the players
     * @param particle the particle
     * @param at where
     * @param count how many
     * @param spread how far they scatter on each axis, in blocks
     * @param speed their speed
     * @param data the particle's data, or null
     * @param direction the way they fly in the world, or null to scatter
     */
    public static void send(
            final List<Player> viewers,
            final Particle particle,
            final Location at,
            final int count,
            final double spread,
            final double speed,
            final @Nullable Object data,
            final @Nullable Vector3f direction
    ) {
        for (Player viewer : viewers) {
            if (direction == null) {
                viewer.spawnParticle(particle, at.getX(), at.getY(), at.getZ(), count, spread, spread, spread, speed, data);
                continue;
            }

            // A count of 0 makes the offset a direction and the speed how fast it flies.
            for (int i = 0; i < count; i++) {
                viewer.spawnParticle(particle, at.getX(), at.getY(), at.getZ(), 0, direction.x, direction.y, direction.z, speed, data);
            }
        }
    }
}
