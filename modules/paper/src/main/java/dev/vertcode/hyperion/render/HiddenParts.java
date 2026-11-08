package dev.vertcode.hyperion.render;

import java.util.Set;

import dev.vertcode.hyperion.api.model.EntityPart;

/**
 * What hiding parts of an entity does to the flags byte its viewers get.
 */
public final class HiddenParts {

    private HiddenParts() {
    }

    /**
     * Change an entity's flags so the hidden parts do not show: the body made invisible, the
     * fire and the outline cleared.
     *
     * @param flags the entity's real flags
     * @param hidden the parts to hide
     * @return the flags to send
     */
    public static byte flags(final byte flags, final Set<EntityPart> hidden) {
        int result = flags;
        if (hidden.contains(EntityPart.BODY)) {
            result |= DisplayData.INVISIBLE;
        }

        if (hidden.contains(EntityPart.FIRE)) {
            result &= ~DisplayData.ON_FIRE;
        }

        if (hidden.contains(EntityPart.GLOW)) {
            result &= ~DisplayData.GLOWING;
        }

        return (byte) result;
    }
}
