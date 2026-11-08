package dev.vertcode.hyperion.api.model.bone;

import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * What an {@link ItemSource} is asked about: which bone, and for which viewer.
 *
 * @param bone the bone the item is for
 * @param viewer the viewer the item is for, or null when one item is shared by every viewer
 */
public record ItemContext(Bone bone, @Nullable Player viewer) {
}
