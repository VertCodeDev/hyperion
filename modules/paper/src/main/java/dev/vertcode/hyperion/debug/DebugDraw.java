package dev.vertcode.hyperion.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.retrooper.packetevents.protocol.player.User;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.Model;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatBehavior;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.hitbox.HitboxBehavior;
import dev.vertcode.hyperion.api.model.locator.Locator;
import dev.vertcode.hyperion.config.DebugSettings;
import dev.vertcode.hyperion.config.HyperionMessages;

import static dev.vertcode.hyperion.config.HyperionMessages.value;

/**
 * Draw the debug views: particles only the debuggers get, and labels through
 * {@link DebugLabels}.
 */
final class DebugDraw {

    private static final double STEP = 0.1d;
    private static final double BOX_STEP = 0.2d;
    private static final float DOT = 1.4f;
    private static final float LINE = 0.6f;
    private static final double LABEL_RISE = 0.3d;
    private static final double SEAT_RISE = 0.6d;
    private static final float ARROW = 1.5f;
    private static final Color PIVOT = Color.WHITE;
    private static final Color AXIS_X = Color.fromRGB(0xFF5555);
    private static final Color AXIS_Y = Color.fromRGB(0x55FF55);
    private static final Color AXIS_Z = Color.fromRGB(0x5599FF);
    private static final Color HITBOX = Color.fromRGB(0x55FF55);
    private static final Color HITBOX_OFF = Color.fromRGB(0x888888);
    private static final Color LOCATOR = Color.fromRGB(0xFF7FE5);
    private static final Color SEAT = Color.fromRGB(0x55FFFF);
    private static final Color SEAT_TAKEN = Color.fromRGB(0xFFAA00);
    private static final Color ANCHOR = Color.WHITE;
    private static final Color AT = Color.fromRGB(0xFFFF55);
    private static final Color PART = Color.fromRGB(0xFFAA55);
    private static final Vector3f X = new Vector3f(1f, 0f, 0f);
    private static final Vector3f Y = new Vector3f(0f, 1f, 0f);
    private static final Vector3f Z = new Vector3f(0f, 0f, 1f);
    // Bones and locators face north as in Blockbench, so their forward is negative Z.
    private static final Vector3f FORWARD = new Vector3f(0f, 0f, -1f);

    private DebugDraw() {
    }

    /**
     * Draw a model's views for the debuggers who see it.
     *
     * @param model the model
     * @param box the box around the whole model when it has one, in the world
     * @param debuggers who sees it with a view on
     * @param labels where names go, or null without labels
     * @param settings how to draw
     */
    static void model(
            final Model model,
            final @Nullable BoundingBox box,
            final List<Debugger> debuggers,
            final @Nullable DebugLabels labels,
            final DebugSettings settings
    ) {
        Seen bones = Seen.of(debuggers, DebugView.BONES);
        Seen hitboxes = Seen.of(debuggers, DebugView.HITBOXES);
        Seen seats = Seen.of(debuggers, DebugView.SEATS);
        for (Bone bone : model.bones()) {
            Location at = bone.location();
            if (!bones.isEmpty()) {
                bone(bones, bone, at, labels, settings.axisLength());
            }

            if (!hitboxes.isEmpty()) {
                bone.behavior(HitboxBehavior.class).ifPresent(hitbox -> box(
                        hitboxes,
                        DebugShapes.hitbox(at.toVector(), bone.rotation(), hitbox.hitbox(), model.scale()),
                        hitbox.interactable() ? HITBOX : HITBOX_OFF
                ));
            }

            if (!seats.isEmpty()) {
                bone.behavior(SeatBehavior.class).ifPresent(seat -> seat(seats, bone, seat, at, labels));
            }
        }

        if (box != null && !hitboxes.isEmpty()) {
            box(hitboxes, box, HITBOX);
        }

        Seen locators = Seen.of(debuggers, DebugView.LOCATORS);
        if (!locators.isEmpty()) {
            for (Locator locator : model.locators()) {
                locator(locators, locator, labels, settings.axisLength());
            }
        }
    }

    /**
     * Draw a running effect for the debuggers who see it.
     *
     * @param effect the effect as it is this tick
     * @param debuggers who sees it with the effects view on
     * @param labels where names go, or null without labels
     */
    static void effect(final DebugEffect effect, final List<Debugger> debuggers, final @Nullable DebugLabels labels) {
        Seen seen = Seen.of(debuggers, DebugView.EFFECTS);
        if (seen.isEmpty()) {
            return;
        }

        dot(seen, effect.anchor(), ANCHOR);
        label(labels, "anchor", effect.anchor(), LABEL_RISE, seen, HyperionMessages.DEBUG__LABEL_EFFECT.component(
                value("effect", effect.key()),
                value("tick", effect.tick()),
                value("duration", effect.duration())
        ));
        for (Map.Entry<String, Location> at : effect.ats().entrySet()) {
            dot(seen, at.getValue(), AT);
            label(labels, "at:" + at.getKey(), at.getValue(), LABEL_RISE, seen,
                    HyperionMessages.DEBUG__LABEL_AT.component(value("at", at.getKey())));
        }

        for (Map.Entry<String, Location> part : effect.parts().entrySet()) {
            dot(seen, part.getValue(), PART);
            label(labels, "part:" + part.getKey(), part.getValue(), -LABEL_RISE, seen,
                    HyperionMessages.DEBUG__LABEL_PART.component(value("part", part.getKey())));
        }
    }

    // Views

    private static void bone(
            final Seen seen,
            final Bone bone,
            final Location at,
            final @Nullable DebugLabels labels,
            final float length
    ) {
        Vector origin = at.toVector();
        Quaternionfc rotation = bone.rotation();
        dot(seen, at, PIVOT);
        line(seen, origin, DebugShapes.along(origin, rotation, X, length), AXIS_X);
        line(seen, origin, DebugShapes.along(origin, rotation, Y, length), AXIS_Y);
        line(seen, origin, DebugShapes.along(origin, rotation, Z, length), AXIS_Z);
        label(labels, "bone:" + bone.name(), at, LABEL_RISE, seen,
                HyperionMessages.DEBUG__LABEL_BONE.component(value("bone", bone.name())));
    }

    private static void seat(
            final Seen seen,
            final Bone bone,
            final SeatBehavior seat,
            final Location at,
            final @Nullable DebugLabels labels
    ) {
        Entity rider = seat.passenger().orElse(null);
        dot(seen, at, rider == null ? SEAT : SEAT_TAKEN);
        Component text = rider == null
                ? HyperionMessages.DEBUG__LABEL_SEAT.component(value("seat", bone.name()))
                : HyperionMessages.DEBUG__LABEL_SEAT_TAKEN.component(value("seat", bone.name()), value("rider", rider.getName()));
        label(labels, "seat:" + bone.name(), at, SEAT_RISE, seen, text);
    }

    private static void locator(final Seen seen, final Locator locator, final @Nullable DebugLabels labels, final float length) {
        Location at = locator.location();
        Vector origin = at.toVector();
        dot(seen, at, LOCATOR);
        line(seen, origin, DebugShapes.along(origin, locator.rotation(), FORWARD, length * ARROW), LOCATOR);
        label(labels, "locator:" + locator.name(), at, LABEL_RISE, seen,
                HyperionMessages.DEBUG__LABEL_LOCATOR.component(value("locator", locator.name())));
    }

    // Drawing

    private static void box(final Seen seen, final BoundingBox box, final Color color) {
        for (Vector point : DebugShapes.edges(box, BOX_STEP)) {
            particle(seen, point.getX(), point.getY(), point.getZ(), color, LINE);
        }
    }

    private static void line(final Seen seen, final Vector from, final Vector to, final Color color) {
        for (Vector point : DebugShapes.line(from, to, STEP)) {
            particle(seen, point.getX(), point.getY(), point.getZ(), color, LINE);
        }
    }

    private static void dot(final Seen seen, final Location at, final Color color) {
        particle(seen, at.getX(), at.getY(), at.getZ(), color, DOT);
    }

    private static void particle(
            final Seen seen,
            final double x,
            final double y,
            final double z,
            final Color color,
            final float size
    ) {
        Particle.DustOptions dust = new Particle.DustOptions(color, size);
        for (Player player : seen.players()) {
            player.spawnParticle(Particle.DUST, x, y, z, 1, 0d, 0d, 0d, 0d, dust);
        }
    }

    private static void label(
            final @Nullable DebugLabels labels,
            final String key,
            final Location at,
            final double rise,
            final Seen seen,
            final Component text
    ) {
        if (labels != null) {
            labels.show(key, at.clone().add(0d, rise, 0d), text, seen.users());
        }
    }

    /**
     * The debuggers with one view on.
     *
     * @param players who they are
     * @param users their connections
     */
    private record Seen(List<Player> players, List<User> users) {

        static Seen of(final List<Debugger> debuggers, final DebugView view) {
            List<Player> players = new ArrayList<>(debuggers.size());
            List<User> users = new ArrayList<>(debuggers.size());
            for (Debugger debugger : debuggers) {
                if (debugger.has(view)) {
                    players.add(debugger.player());
                    users.add(debugger.user());
                }
            }

            return new Seen(players, users);
        }

        boolean isEmpty() {
            return this.players.isEmpty();
        }
    }
}
