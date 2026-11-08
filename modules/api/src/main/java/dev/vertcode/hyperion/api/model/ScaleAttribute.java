package dev.vertcode.hyperion.api.model;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.jspecify.annotations.Nullable;

import dev.vertcode.hyperion.api.version.Feature;

/**
 * The scale attribute, looked up once by the key the running version uses.
 *
 * <p>Paper's constant for it is missing on 1.21 and 1.21.1, so Hyperion asks the registry
 * instead of naming the constant.
 */
final class ScaleAttribute {

    private static final @Nullable Attribute ATTRIBUTE = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(
            Feature.PLAIN_ATTRIBUTE_KEYS.available() ? "scale" : "generic.scale"
    ));

    private ScaleAttribute() {
    }

    /**
     * Return the scale attribute of the running server.
     *
     * @return the attribute, or null on a server without one
     */
    static @Nullable Attribute get() {
        return ATTRIBUTE;
    }
}
