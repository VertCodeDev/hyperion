package dev.vertcode.hyperion.skin;

import java.util.Locale;

/**
 * Which skins a cube of a player model is drawn for: every skin, or only those with the slim or
 * the wide arms.
 *
 * <p>A player model has both arm shapes in its file. A cube or bone whose name ends in
 * {@code _slim} shows for slim skins only, one ending in {@code _wide} or {@code _classic} for
 * the others.
 */
public enum SkinVariant {

    /**
     * Drawn for every skin.
     */
    ANY,

    /**
     * Drawn only for a skin with the slim arms.
     */
    SLIM,

    /**
     * Drawn only for a skin with the wide arms.
     */
    WIDE;

    private static final String SLIM_SUFFIX = "_slim";
    private static final String WIDE_SUFFIX = "_wide";
    private static final String CLASSIC_SUFFIX = "_classic";

    /**
     * Read the variant from the name of a cube or a bone.
     *
     * @param name the name
     * @return the variant its suffix asks for, {@link #ANY} without one
     */
    public static SkinVariant ofName(final String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(SLIM_SUFFIX)) {
            return SLIM;
        }

        return lower.endsWith(WIDE_SUFFIX) || lower.endsWith(CLASSIC_SUFFIX) ? WIDE : ANY;
    }

    /**
     * Return whether a cube of this variant is drawn for a skin.
     *
     * @param slim whether the skin has the slim arms
     * @return true when it is drawn
     */
    public boolean shows(final boolean slim) {
        return this == ANY || (this == SLIM) == slim;
    }
}
