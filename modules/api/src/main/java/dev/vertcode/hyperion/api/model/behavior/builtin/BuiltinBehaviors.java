package dev.vertcode.hyperion.api.model.behavior.builtin;

import net.kyori.adventure.key.Key;

/**
 * The keys of the behaviors Hyperion ships, and the bone names that trigger them.
 *
 * <p>A bone gets a behavior when its name starts with one of the behavior's prefixes, ignoring
 * case. The prefixes below are the defaults; server owners change them in the {@code behaviors}
 * section of {@code config.yml}, for instance to add the names another engine uses. A bone can
 * also carry the key as a tag (see {@code BehaviorMatcher.TAG_FIELD}), such as
 * {@code hyperion:seat}, whatever its name.
 */
public final class BuiltinBehaviors {

    /**
     * Turns with the entity's head, as a {@link HeadBehavior}. Bones named {@code head*}.
     */
    public static final Key HEAD = Key.key("hyperion", "head");

    /**
     * A hitbox players can hit and click, as a {@code HitboxBehavior}. Its own cubes set its
     * size and are not drawn. Bones named {@code hitbox*}.
     */
    public static final Key HITBOX = Key.key("hyperion", "hitbox");

    /**
     * A seat an entity can ride, as a {@link SeatBehavior}. Bones named {@code seat*}.
     */
    public static final Key SEAT = Key.key("hyperion", "seat");

    /**
     * A name tag that follows the bone, as a {@link NametagBehavior}. Bones named
     * {@code nametag*}.
     */
    public static final Key NAMETAG = Key.key("hyperion", "nametag");

    /**
     * Shows what the carrying entity holds in its main hand, as a {@link HandBehavior}. Bones
     * named {@code main_hand*}.
     */
    public static final Key MAIN_HAND = Key.key("hyperion", "main_hand");

    /**
     * Shows what the carrying entity holds in its off hand, as a {@link HandBehavior}. Bones
     * named {@code off_hand*}.
     */
    public static final Key OFF_HAND = Key.key("hyperion", "off_hand");

    /**
     * A bone that is never drawn, kept for logic and locators, as a {@link GhostBehavior}.
     * Bones named {@code ghost*}.
     */
    public static final Key GHOST = Key.key("hyperion", "ghost");

    private BuiltinBehaviors() {
    }
}
