package dev.vertcode.hyperion.effect.part;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.effect.EffectActor;
import dev.vertcode.hyperion.api.effect.EffectContext;
import dev.vertcode.hyperion.api.effect.EffectFrame;
import dev.vertcode.hyperion.effect.part.Shapes.Shape;

/**
 * A particle emitter: every {@code every} ticks while visible, it sends its shape of particles.
 *
 * <p>Values: {@code particle}, {@code shape} ({@code point}, {@code ring}, {@code helix},
 * {@code sphere}, {@code line}), {@code count}, {@code radius}, {@code height} for a helix,
 * {@code to} for the end of a line, {@code spread} and {@code speed}, {@code spin} for how fast a
 * ring or helix turns, {@code direction} to make particles fly one way, and {@code color},
 * {@code to-color} and {@code size} for dust. A part with keyframes waits for its first.
 */
final class ParticleActor implements EffectActor {

    private static final int DEFAULT_COUNT = 8;
    private static final int DEFAULT_EVERY = 2;
    private static final float DEFAULT_SPREAD = 0.1f;
    private static final float DEFAULT_SPIN = 0.2f;
    private static final float DEFAULT_HEIGHT = 2f;

    private final EffectContext context;
    private @Nullable Location here;

    /**
     * Create the emitter of one part.
     *
     * @param context the effect it plays in
     */
    ParticleActor(final EffectContext context) {
        this.context = context;
    }

    @Override
    public void frame(final int tick, final EffectFrame frame) {
        Location center = this.context.location(frame);
        this.here = center;
        if (center == null || !frame.bool("visible", true)) {
            return;
        }

        int every = Math.max(1, Math.round(frame.number("every", DEFAULT_EVERY)));
        Particle particle = ParticleData.particle(frame.string("particle", "flame"));
        if (tick % every != 0 || particle == null || !ParticleData.supported(particle)) {
            return;
        }

        int count = this.context.particles(Math.round(frame.number("count", DEFAULT_COUNT)));
        List<Player> viewers = count > 0 ? this.context.viewers() : List.of();
        if (viewers.isEmpty()) {
            return;
        }

        Object data = ParticleData.data(particle, frame);
        float spread = frame.number("spread", DEFAULT_SPREAD);
        float speed = frame.number("speed", 0f);
        Vector3f direction = this.direction(frame);
        Shape shape = Shape.of(frame.string("shape", "point"));
        if (shape == Shape.POINT) {
            ParticleData.send(viewers, particle, center, count, spread, speed, data, direction);
            return;
        }

        for (Vector3f point : this.points(shape, count, tick, frame)) {
            Vector3f offset = this.context.turn(frame, point.x, point.y, point.z);
            Location at = center.clone().add(offset.x, offset.y, offset.z);
            ParticleData.send(viewers, particle, at, 1, 0d, speed, data, direction);
        }
    }

    /**
     * Work out the points of a shape, around the center.
     *
     * @param shape the shape
     * @param count how many points
     * @param tick the effect's tick, which turns rings and helixes
     * @param frame the values
     * @return the points, to the right, up and forward
     */
    private List<Vector3f> points(final Shape shape, final int count, final int tick, final EffectFrame frame) {
        float radius = frame.number("radius", 1f);
        float phase = tick * frame.number("spin", DEFAULT_SPIN);

        return switch (shape) {
            case RING -> Shapes.ring(count, radius, phase);
            case HELIX -> Shapes.helix(count, radius, frame.number("height", DEFAULT_HEIGHT), phase);
            case SPHERE -> Shapes.sphere(count, radius);
            case LINE -> {
                Vector3f end = frame.vector("to");
                yield Shapes.line(count, end == null ? new Vector3f(0f, 0f, 1f) : end);
            }
            case POINT -> List.of(new Vector3f());
        };
    }

    /**
     * Turn the frame's {@code direction} into the world, when it has one.
     *
     * @param frame the values
     * @return the direction in the world, or null to scatter
     */
    private @Nullable Vector3f direction(final EffectFrame frame) {
        Vector3f local = frame.vector("direction");

        return local == null ? null : this.context.turn(frame, local.x, local.y, local.z);
    }

    @Override
    public @Nullable Location location() {
        Location at = this.here;

        return at == null ? null : at.clone();
    }
}
