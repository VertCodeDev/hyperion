package dev.vertcode.hyperion.api.model.behavior.builtin;

import net.kyori.adventure.key.Key;

/**
 * The keys of the behaviors Hyperion ships, and the bone names that trigger them.
 *
 * <p>The prefixes follow the names Blockbench users already know from other engines, so
 * existing models work without renaming. Each one can also be set as a tag (see
 * {@code BehaviorMatcher.TAG_FIELD}).
 */
public final class BuiltinBehaviors {

    /**
     * Turns with the entity's head. Bones named {@code h_*} or {@code head}.
     */
    public static final Key HEAD = Key.key("hyperion", "head");

    /**
     * A hitbox players can hit and click. Bones named {@code hitbox} or {@code b_*}.
     */
    public static final Key HITBOX = Key.key("hyperion", "hitbox");

    /**
     * A seat an entity can ride. Bones named {@code p_*} or {@code seat*}.
     */
    public static final Key SEAT = Key.key("hyperion", "seat");

    /**
     * A name tag that follows the bone. Bones named {@code tag_*}.
     */
    public static final Key NAMETAG = Key.key("hyperion", "nametag");

    /**
     * A bone that never renders, kept for logic and locators. Bones named {@code ghost_*}.
     */
    public static final Key GHOST = Key.key("hyperion", "ghost");

    private BuiltinBehaviors() {
    }
}
