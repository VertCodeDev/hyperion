package dev.vertcode.hyperion.behavior;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.model.EntityAnchor;
import dev.vertcode.hyperion.api.model.behavior.BehaviorMatcher;
import dev.vertcode.hyperion.api.model.behavior.BehaviorType;
import dev.vertcode.hyperion.api.model.behavior.BoneBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.BuiltinBehaviors;
import dev.vertcode.hyperion.api.model.behavior.builtin.GhostBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.HandBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.HeadBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.NametagBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatBehavior;
import dev.vertcode.hyperion.api.model.behavior.builtin.SeatController;
import dev.vertcode.hyperion.api.model.bone.Bone;
import dev.vertcode.hyperion.api.model.bone.BoneBlueprint;
import dev.vertcode.hyperion.api.model.hitbox.HitboxBehavior;
import dev.vertcode.hyperion.config.BehaviorSettings;
import dev.vertcode.hyperion.config.SeatSteering;
import dev.vertcode.hyperion.model.ModelManager;

/**
 * The behaviors Hyperion ships: head, hitbox, seat, name tag, hands and ghost.
 *
 * <p>Each is matched by the bone name prefixes in {@code config.yml}, read as a model spawns,
 * or by its key as a tag on the bone. They are registered under Hyperion like any plugin's type.
 */
public final class BuiltinTypes {

    private BuiltinTypes() {
    }

    /**
     * Register every built-in behavior.
     *
     * @param plugin Hyperion, which owns them
     * @param registry where behavior types are registered
     * @param models the live models, for the settings and the lookups the behaviors fill
     */
    public static void register(final Plugin plugin, final HyperionBehaviorRegistry registry, final ModelManager models) {
        Supplier<BehaviorSettings> settings = () -> models.settings().behaviors();
        NamespacedKey seatKey = new NamespacedKey(plugin, "seat");
        BehaviorMatcher seats = matcher(BuiltinBehaviors.SEAT, settings);

        register(plugin, registry, BuiltinBehaviors.HEAD, HeadBehavior.class, settings, bone -> new HyperionHead(
                bone,
                settings.get().headMaxYaw(),
                settings.get().headMaxPitch()
        ));
        register(plugin, registry, BuiltinBehaviors.HITBOX, HitboxBehavior.class, settings, bone -> new HyperionHitbox(
                bone,
                models.interactions(),
                settings.get().forwardDamage()
        ));
        registry.register(plugin, BehaviorType.of(BuiltinBehaviors.SEAT, SeatBehavior.class, seats, bone -> new HyperionSeat(
                bone,
                models.interactions(),
                seatKey,
                steering(bone, seats, settings.get().steering())
        )));
        register(plugin, registry, BuiltinBehaviors.NAMETAG, NametagBehavior.class, settings, HyperionNametag::new);
        register(plugin, registry, BuiltinBehaviors.MAIN_HAND, HandBehavior.class, settings, bone -> new HyperionHand(bone, EquipmentSlot.HAND));
        register(plugin, registry, BuiltinBehaviors.OFF_HAND, HandBehavior.class, settings, bone -> new HyperionHand(bone, EquipmentSlot.OFF_HAND));
        register(plugin, registry, BuiltinBehaviors.GHOST, GhostBehavior.class, settings, bone -> new HyperionGhost());
    }

    /**
     * Tell which bones never have their own cubes drawn: ghosts, and hitboxes, whose cubes only
     * give their size. The Bedrock pack leaves those cubes out.
     *
     * @param settings the settings in use, read at every call
     * @return the test
     */
    public static Predicate<BoneBlueprint> hidesCubes(final Supplier<BehaviorSettings> settings) {
        BehaviorMatcher hidden = matcher(BuiltinBehaviors.GHOST, settings).or(matcher(BuiltinBehaviors.HITBOX, settings));

        return hidden::matches;
    }

    /**
     * Register one built-in behavior, matched by its prefixes or its tag.
     *
     * @param plugin Hyperion
     * @param registry where behavior types are registered
     * @param key the behavior's key
     * @param type the behavior's interface
     * @param settings the settings in use
     * @param factory creates the behavior for one bone
     * @param <B> the behavior's type
     */
    private static <B extends BoneBehavior> void register(
            final Plugin plugin,
            final HyperionBehaviorRegistry registry,
            final Key key,
            final Class<B> type,
            final Supplier<BehaviorSettings> settings,
            final Function<Bone, B> factory
    ) {
        registry.register(plugin, BehaviorType.of(key, type, matcher(key, settings), factory));
    }

    /**
     * Match bones by a behavior's prefixes in {@code config.yml}, or its key as a tag.
     *
     * @param key the behavior's key
     * @param settings the settings in use, read at every match
     * @return the matcher
     */
    private static BehaviorMatcher matcher(final Key key, final Supplier<BehaviorSettings> settings) {
        BehaviorMatcher named = bone -> settings.get().matches(key, bone.name());

        return named.or(BehaviorMatcher.tagged(key.asString()));
    }

    /**
     * Pick the controller a seat starts with: the first seat of a model on an entity steers when
     * {@code config.yml} turns steering on for the entity's type.
     *
     * @param bone the seat's bone
     * @param seats which bones are seats
     * @param steering the steering settings
     * @return the controller, or null to only ride along
     */
    private static @Nullable SeatController steering(final Bone bone, final BehaviorMatcher seats, final SeatSteering steering) {
        if (!(bone.model().anchor() instanceof EntityAnchor(Entity entity)) || !steering.enabledFor(entity.getType())) {
            return null;
        }

        for (Bone each : bone.model().bones()) {
            if (seats.matches(each.blueprint())) {
                if (!each.name().equals(bone.name())) {
                    return null;
                }

                break;
            }
        }

        return steering.flying()
                ? SeatController.flying(steering.speed())
                : SeatController.walking(steering.speed(), steering.jump());
    }
}
