package dev.vertcode.hyperion.armor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import dev.vertcode.hyperion.api.armor.ArmorSlot;
import dev.vertcode.hyperion.blueprint.data.Texture;

/**
 * What each texture of an armor set is for, by its name in Blockbench.
 *
 * <ul>
 *   <li>{@code flat_outer} and {@code flat_inner}: the flat look, drawn where no 3D shows, on
 *       vanilla's outer and inner armor texture. Each may have a {@code _tint} and a
 *       {@code _glow} version.</li>
 *   <li>{@code icon_<piece>}, such as {@code icon_helmet}: the piece's flat inventory icon.</li>
 *   <li>Any other texture is the model's own. A name ending in {@code _tint} takes the dye color,
 *       and one ending in {@code _glow} lights up in the dark.</li>
 * </ul>
 */
public final class ArmorTextures {

    private static final String TINT = "_tint";
    private static final String GLOW = "_glow";
    private static final String ICON = "icon_";
    private static final String PNG = ".png";

    private final Map<String, Integer> named = new HashMap<>();
    private final Set<Integer> tinted = new HashSet<>();
    private final Set<Integer> glowing = new HashSet<>();

    /**
     * Sort a set's textures by name.
     *
     * @param textures the textures, in file order
     */
    public ArmorTextures(final List<Texture> textures) {
        for (int index = 0; index < textures.size(); index++) {
            String name = clean(textures.get(index).name());
            this.named.putIfAbsent(name, index);
            if (name.endsWith(TINT)) {
                this.tinted.add(index);
            }

            if (name.endsWith(GLOW)) {
                this.glowing.add(index);
            }
        }
    }

    /**
     * Return the flat texture of a layer.
     *
     * @param layer the layer
     * @return its index, or empty when the set has none
     */
    public OptionalInt flat(final ArmorLayer layer) {
        return this.find(layer.texture());
    }

    /**
     * Return the part of a layer's flat look that takes the dye color.
     *
     * @param layer the layer
     * @return its index, or empty when the set has none
     */
    public OptionalInt flatTint(final ArmorLayer layer) {
        return this.find(layer.texture() + TINT);
    }

    /**
     * Return the part of a layer's flat look that lights up.
     *
     * @param layer the layer
     * @return its index, or empty when the set has none
     */
    public OptionalInt flatGlow(final ArmorLayer layer) {
        return this.find(layer.texture() + GLOW);
    }

    /**
     * Return a piece's flat inventory icon.
     *
     * @param slot the piece's slot
     * @return its index, or empty when the piece gets a 3D icon instead
     */
    public OptionalInt icon(final ArmorSlot slot) {
        return this.find(ICON + slot.piece());
    }

    /**
     * Return whether faces with a texture take the dye color.
     *
     * @param index the texture's index
     * @return true when its name ends in {@code _tint}
     */
    public boolean tinted(final int index) {
        return this.tinted.contains(index);
    }

    /**
     * Return whether faces with a texture light up in the dark.
     *
     * @param index the texture's index
     * @return true when its name ends in {@code _glow}
     */
    public boolean glowing(final int index) {
        return this.glowing.contains(index);
    }

    /**
     * Find a texture by its cleaned name.
     *
     * @param name the name
     * @return the index, or empty when there is none
     */
    private OptionalInt find(final String name) {
        Integer index = this.named.get(name);

        return index == null ? OptionalInt.empty() : OptionalInt.of(index);
    }

    /**
     * Clean a texture's name: lowercase, without {@code .png}.
     *
     * @param name the name as saved
     * @return the cleaned name
     */
    static String clean(final String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);

        return lower.endsWith(PNG) ? lower.substring(0, lower.length() - PNG.length()) : lower;
    }
}
