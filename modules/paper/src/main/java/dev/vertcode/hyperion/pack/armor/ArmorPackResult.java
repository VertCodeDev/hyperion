package dev.vertcode.hyperion.pack.armor;

import dev.vertcode.hyperion.armor.ArmorPackInfo;

/**
 * What writing the armor into a pack gave.
 *
 * @param info the numbers armor items need
 * @param overlay whether files went into the overlay for the server's exact pack format
 */
public record ArmorPackResult(ArmorPackInfo info, boolean overlay) {

    /**
     * The overlay folder, which only clients of the server's own version read.
     */
    public static final String OVERLAY = "hyperion_armor";

    /**
     * Return the result of a pack without armor.
     *
     * @return the empty result
     */
    public static ArmorPackResult empty() {
        return new ArmorPackResult(ArmorPackInfo.empty(), false);
    }
}
