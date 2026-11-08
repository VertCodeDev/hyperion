package dev.vertcode.hyperion.api.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A part of the entity carrying a model that can be hidden from viewers, so only the model shows.
 *
 * <p>Hiding is done in the packets each viewer gets; the entity itself does not change.
 */
public enum EntityPart {

    /**
     * The entity's own body.
     */
    BODY,

    /**
     * Armor and held items.
     */
    EQUIPMENT,

    /**
     * The name tag above the entity.
     */
    NAME,

    /**
     * The fire drawn on a burning entity.
     */
    FIRE,

    /**
     * The glowing outline, so only the model's own glow shows.
     */
    GLOW;

    /**
     * Every part.
     */
    public static final Set<EntityPart> ALL = Collections.unmodifiableSet(EnumSet.allOf(EntityPart.class));

    /**
     * No part.
     */
    public static final Set<EntityPart> NONE = Collections.unmodifiableSet(EnumSet.noneOf(EntityPart.class));
}
