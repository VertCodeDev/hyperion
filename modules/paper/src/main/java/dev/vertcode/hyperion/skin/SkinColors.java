package dev.vertcode.hyperion.skin;

import java.util.List;

/**
 * What draws one skin on one model part: a color for every skin pixel the part shows, and a
 * flag for every pixel that is drawn only for some skins.
 *
 * @param colors the colors as opaque ARGB, in the order the pack numbered the pixels
 * @param flags whether each optional pixel is drawn, in the same order as
 *     {@link SkinMap#optional()}
 */
public record SkinColors(List<Integer> colors, List<Boolean> flags) {

    /**
     * Copy the lists, so the colors never change after they are worked out.
     */
    public SkinColors {
        colors = List.copyOf(colors);
        flags = List.copyOf(flags);
    }
}
